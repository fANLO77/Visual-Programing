package com.example.android_project

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.CellIdentity
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CellLocationService : Service() {
    companion object {
        @Volatile var lastCellInfoText: String = "Нет данных о вышках"
        @Volatile var lastUpdateTime: Long = 0
        private const val CHANNEL_ID = "cell_service_channel"
        private const val NOTIF_ID = 1001
        private const val UPDATE_INTERVAL = 30000L
    }
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null
    private val handler = Handler(Looper.getMainLooper())
    private var periodicRunnable: Runnable? = null
    private val zmqContext = ZContext()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val SERVER_IP = "172.31.53.226"
    private val SERVER_PORT = "5555"
    private val TAG = "CellServiceLog"
    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        startLocationUpdates()
        startPeriodicSending()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cell & Location Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновый сервис сбора локации и данных о сотовых сетях"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Сбор данных о сети и локации")
            .setContentText("Работает в фоне")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                lastLocation = result.lastLocation
            }
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
        }
    }
    private fun startPeriodicSending() {
        periodicRunnable = Runnable {
            lastLocation?.let { loc ->
                try {
                    val json = buildFullJson()
                    sendToBackend(json)
                    writeFullJsonToLocal(json)
                    updateCellInfoText()
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка в периодической задаче", e)
                }
            }
            handler.postDelayed(periodicRunnable!!, UPDATE_INTERVAL)
        }
        handler.post(periodicRunnable!!)
    }
    private fun buildFullJson(): JSONObject {
        val json = JSONObject()
        lastLocation?.let { loc ->
            json.put("latitude", loc.latitude)
            json.put("longitude", loc.longitude)
            json.put("altitude", if (loc.hasAltitude()) loc.altitude else 0.0)
            json.put("currentTime", loc.time)
            json.put("accuracy", loc.accuracy)
            json.put("readableTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(loc.time)))
        }
        val cells = JSONArray()
        val hasPhonePerm = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPhonePerm) {
            telephonyManager.allCellInfo?.forEach { cellInfo ->
                val cell = JSONObject()
                when {
                    cellInfo is CellInfoLte -> {
                        val id = cellInfo.cellIdentity as CellIdentityLte
                        val sig = cellInfo.cellSignalStrength as CellSignalStrengthLte
                        cell.put("type", "lte")
                        cell.put("band", getBand(id))
                        cell.put("cellIdentity", id.ci)
                        cell.put("earfcn", id.earfcn)
                        cell.put("mcc", getMccString(id))
                        cell.put("mnc", getMncString(id))
                        cell.put("pci", id.pci)
                        cell.put("tac", id.tac)
                        cell.put("asuLevel", sig.asuLevel)
                        cell.put("cqi", sig.cqi)
                        cell.put("rsrp", sig.rsrp)
                        cell.put("rsrq", sig.rsrq)
                        cell.put("rssi", if (Build.VERSION.SDK_INT >= 29) sig.rssi else -1)
                        cell.put("rssnr", sig.rssnr)
                        cell.put("timingAdvance", sig.timingAdvance)
                    }
                    cellInfo is CellInfoGsm -> {
                        val id = cellInfo.cellIdentity as CellIdentityGsm
                        val sig = cellInfo.cellSignalStrength as CellSignalStrengthGsm
                        cell.put("type", "gsm")
                        cell.put("cellIdentity", id.cid)
                        cell.put("bsic", id.bsic)
                        cell.put("arfcn", id.arfcn)
                        cell.put("lac", id.lac)
                        cell.put("mcc", getMccString(id))
                        cell.put("mnc", getMncString(id))
                        cell.put("psc", id.psc)
                        cell.put("dbm", sig.dbm)
                        cell.put("rssi", if (Build.VERSION.SDK_INT >= 30) sig.rssi else -1)
                        cell.put("timingAdvance", sig.timingAdvance)
                    }
                    Build.VERSION.SDK_INT >= 29 && cellInfo is CellInfoNr -> {
                        val id = cellInfo.cellIdentity as CellIdentityNr
                        val sig = cellInfo.cellSignalStrength as CellSignalStrengthNr
                        cell.put("type", "nr")
                        cell.put("band", getBand(id))
                        cell.put("nci", id.nci)
                        cell.put("pci", id.pci)
                        cell.put("nrArfcn", id.nrarfcn)
                        cell.put("tac", id.tac)
                        cell.put("mcc", getMccString(id))
                        cell.put("mnc", getMncString(id))
                        cell.put("ssRsrp", sig.ssRsrp)
                        cell.put("ssRsrq", sig.ssRsrq)
                        cell.put("ssSinr", sig.ssSinr)
                        cell.put("timingAdvance", if (Build.VERSION.SDK_INT >= 34) sig.timingAdvanceMicros else -1L)
                    }
                }
                if (cell.length() > 0) cells.put(cell)
            }
        }
        json.put("cells", cells)
        json.put("totalRxBytes", TrafficStats.getTotalRxBytes())
        json.put("totalTxBytes", TrafficStats.getTotalTxBytes())
        json.put("mobileRxBytes", TrafficStats.getMobileRxBytes())

        return json
    }
    private fun updateCellInfoText() {
        val sb = StringBuilder("Данные о сетях (${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}):\n\n")

        val hasPerm = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) {
            lastCellInfoText = "Нет разрешений READ_PHONE_STATE / ACCESS_COARSE_LOCATION"
            lastUpdateTime = System.currentTimeMillis()
            return
        }
        val cellInfoList = telephonyManager.allCellInfo ?: emptyList()
        if (cellInfoList.isEmpty()) {
            sb.append("Нет доступных вышек\n")
        } else {
            cellInfoList.forEachIndexed { index, info ->
                sb.append("Вышка #${index + 1}:\n")
                when (info) {
                    is CellInfoLte -> {
                        val id = info.cellIdentity as CellIdentityLte
                        val sig = info.cellSignalStrength as CellSignalStrengthLte
                        sb.append("  Тип: LTE\n")
                        sb.append("  PCI: ${id.pci}, TAC: ${id.tac}, CI: ${id.ci}\n")
                        sb.append("  EARFCN: ${id.earfcn}, MCC: ${getMccString(id)}, MNC: ${getMncString(id)}\n")
                        sb.append("  RSRP: ${sig.rsrp} dBm, RSRQ: ${sig.rsrq}, TA: ${sig.timingAdvance}\n")
                    }
                    is CellInfoGsm -> {
                        val id = info.cellIdentity as CellIdentityGsm
                        val sig = info.cellSignalStrength as CellSignalStrengthGsm
                        sb.append("  Тип: GSM\n")
                        sb.append("  LAC: ${id.lac}, CID: ${id.cid}, ARFCN: ${id.arfcn}, BSIC: ${id.bsic}\n")
                        sb.append("  MCC: ${getMccString(id)}, MNC: ${getMncString(id)}\n")
                        sb.append("  Уровень: ${sig.dbm} dBm\n")
                    }
                    is CellInfoNr -> if (Build.VERSION.SDK_INT >= 29) {
                        val id = info.cellIdentity as CellIdentityNr
                        val sig = info.cellSignalStrength as CellSignalStrengthNr
                        sb.append("  Тип: 5G NR\n")
                        sb.append("  PCI: ${id.pci}, TAC: ${id.tac}, NCI: ${id.nci}\n")
                        sb.append("  NR-ARFCN: ${id.nrarfcn}\n")
                        sb.append("  SS-RSRP: ${sig.ssRsrp}, SS-RSRQ: ${sig.ssRsrq}, SS-SINR: ${sig.ssSinr}\n")
                    }
                }
                sb.append("  Registered: ${info.isRegistered}\n\n")
            }
        }
        lastCellInfoText = sb.toString()
        lastUpdateTime = System.currentTimeMillis()
    }
    private fun getMccString(identity: CellIdentity): String {
        return when (identity) {
            is CellIdentityLte -> if (Build.VERSION.SDK_INT >= 28) identity.mccString ?: "unknown" else identity.mcc.toString()
            is CellIdentityGsm -> if (Build.VERSION.SDK_INT >= 28) identity.mccString ?: "unknown" else identity.mcc.toString()
            is CellIdentityNr -> if (Build.VERSION.SDK_INT >= 29) identity.mccString ?: "unknown" else "unknown"
            else -> "unknown"
        }
    }
    private fun getMncString(identity: CellIdentity): String {
        return when (identity) {
            is CellIdentityLte -> if (Build.VERSION.SDK_INT >= 28) identity.mncString ?: "unknown" else identity.mnc.toString()
            is CellIdentityGsm -> if (Build.VERSION.SDK_INT >= 28) identity.mncString ?: "unknown" else identity.mnc.toString()
            is CellIdentityNr -> if (Build.VERSION.SDK_INT >= 29) identity.mncString ?: "unknown" else "unknown"
            else -> "unknown"
        }
    }
    private fun getBand(identity: CellIdentity): Int {
        if (Build.VERSION.SDK_INT < 30) return -1
        val bands = when (identity) {
            is CellIdentityLte -> identity.bands
            is CellIdentityNr -> identity.bands
            else -> null
        }
        return bands?.firstOrNull() ?: -1
    }
    private fun sendToBackend(json: JSONObject) {
        scope.launch {
            var socket: ZMQ.Socket? = null
            try {
                socket = zmqContext.createSocket(SocketType.REQ)
                socket.receiveTimeOut = 3000
                socket.sendTimeOut = 3000
                val address = "tcp://$SERVER_IP:$SERVER_PORT"
                socket.connect(address)
                val jsonPayload = json.toString()
                val isSent = socket.send(jsonPayload.toByteArray(ZMQ.CHARSET), 0)
                if (isSent) {
                    val response = socket.recvStr(0)
                    Log.d(TAG, "Отправлено на сервер: ${jsonPayload.take(200)}... Ответ: $response")
                } else {
                    Log.e(TAG, "Ошибка отправки")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка ZMQ: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }
    private fun writeFullJsonToLocal(json: JSONObject) {
        val jsonString = json.toString() + "\n"
        try {
            val file = File(filesDir, "location_history.json")
            FileOutputStream(file, true).use { it.write(jsonString.toByteArray()) }
            Log.d(TAG, "Записано в location_history.json")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка записи в файл: ${e.message}")
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        periodicRunnable?.let { handler.removeCallbacks(it) }
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        scope.cancel()
        zmqContext.destroy()
        super.onDestroy()
    }
}