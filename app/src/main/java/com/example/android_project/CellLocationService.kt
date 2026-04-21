package com.example.android_project

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
        private const val CHANNEL_ID = "cell_service_channel"
        private const val NOTIF_ID = 1001
        private const val UPDATE_INTERVAL = 3000L
        const val BROADCAST_ACTION = "CellLocationUpdate"
        private const val DEFAULT_SERVER_IP = "192.168.0.17"
        private const val DEFAULT_SERVER_PORT = "5559"
        private const val TAG = "CellServiceLog"
    }

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null
    private val handler = Handler(Looper.getMainLooper())
    private var periodicRunnable: Runnable? = null

    private val zmqContext = ZContext()
    private var zmqSocket: ZMQ.Socket? = null
    private var lastConnectedIp = ""
    private var lastConnectedPort = ""

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var currentServerIp = DEFAULT_SERVER_IP
    private var currentServerPort = DEFAULT_SERVER_PORT

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Service onCreate()")
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        startLocationUpdates()
        startPeriodicSending()

        initZmqSocket()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val newIp = it.getStringExtra("SERVER_IP") ?: DEFAULT_SERVER_IP
            val newPort = it.getStringExtra("SERVER_PORT") ?: DEFAULT_SERVER_PORT

            Log.d(TAG, "📡 Received config: $newIp:$newPort")

            if (newIp != currentServerIp || newPort != currentServerPort) {
                currentServerIp = newIp
                currentServerPort = newPort
                initZmqSocket()
            }
        }
        return START_STICKY
    }

    private fun initZmqSocket() {
        scope.launch {
            try {
                Log.d(TAG, "🔌 Initializing ZMQ socket to $currentServerIp:$currentServerPort")

                zmqSocket?.close()

                zmqSocket = zmqContext.createSocket(SocketType.PUSH)
                zmqSocket?.connect("tcp://$currentServerIp:$currentServerPort")

                delay(100)

                lastConnectedIp = currentServerIp
                lastConnectedPort = currentServerPort

                Log.d(TAG, "✅ ZMQ socket connected successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ ZMQ init failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Cell Service", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Мониторинг сети")
            .setContentText("Сбор данных запущен")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                lastLocation = res.lastLocation
                Log.d(TAG, "📍 Location updated: ${res.lastLocation?.latitude}, ${res.lastLocation?.longitude}")
            }
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } else {
            Log.e(TAG, "❌ Location permission not granted!")
        }
    }

    private fun startPeriodicSending() {
        periodicRunnable = Runnable {
            try {
                val loc = lastLocation
                if (loc != null) {
                    Log.d(TAG, "📤 Preparing to send data...")
                    val json = buildFullJson(loc)
                    sendToBackend(json.toString())
                    writeToPhoneLog(json.toString())
                    sendBroadcastUpdate(buildCellInfoText(), json.toString(2))
                } else {
                    Log.w(TAG, "⚠️ No location available yet")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in periodic sending: ${e.message}")
            }
            handler.postDelayed(periodicRunnable!!, UPDATE_INTERVAL)
        }
        handler.post(periodicRunnable!!)
    }

    private fun buildFullJson(location: Location): JSONObject {
        val json = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("currentTime", location.time)
            put("readableTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(location.time)))
        }
        val cells = JSONArray()
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val cellInfo = telephonyManager.allCellInfo
            if (cellInfo.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ No cell info available")
            } else {
                Log.d(TAG, "📡 Found ${cellInfo.size} cell(s)")
                cellInfo.forEach { info ->
                    val cell = JSONObject().apply { put("registered", info.isRegistered) }
                    try {
                        when (info) {
                            is CellInfoLte -> {
                                cell.put("type", "lte")
                                cell.put("pci", info.cellIdentity.pci)
                                cell.put("rsrp", info.cellSignalStrength.rsrp)
                                cell.put("rsrq", info.cellSignalStrength.rsrq)
                                cell.put("sinr", info.cellSignalStrength.rssnr / 10.0)
                                val lteId = info.cellIdentity
                                cell.put("mcc", info.cellIdentity.mccString ?: "")
                                cell.put("mnc", info.cellIdentity.mncString ?: "")
                                Log.d(TAG, "📶 LTE PCI: ${info.cellIdentity.pci}, RSRP: ${info.cellSignalStrength.rsrp}")
                            }
                            is CellInfoNr -> {
                                val id = info.cellIdentity as CellIdentityNr
                                val sig = info.cellSignalStrength as CellSignalStrengthNr
                                cell.put("type", "nr")
                                cell.put("pci", id.pci)
                                cell.put("rsrp", sig.ssRsrp)
                                cell.put("rsrq", sig.ssRsrq)
                                cell.put("sinr", sig.ssSinr)
                                cell.put("mcc", id.mccString ?: "")
                                cell.put("mnc", id.mncString ?: "")
                                Log.d(TAG, "📶 5G PCI: ${id.pci}, RSRP: ${sig.ssRsrp}")
                            }
                            is CellInfoGsm -> {
                                cell.put("type", "gsm")
                                cell.put("cid", info.cellIdentity.cid)
                                cell.put("pci", 0)
                                cell.put("rsrp", info.cellSignalStrength.dbm)
                                val gsmId = info.cellIdentity
                                cell.put("mcc", info.cellIdentity.mccString ?: "")
                                cell.put("mnc", info.cellIdentity.mncString ?: "")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing cell info: ${e.message}")
                    }
                    if (cell.has("type")) cells.put(cell)
                }
            }
        }
        json.put("cells", cells)
        return json
    }

    private fun buildCellInfoText(): String {
        val sb = StringBuilder("СПИСОК ВЫШЕК:\n")
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return "Нет разрешения на геолокацию"
        }
        val list = telephonyManager.allCellInfo
        if (list.isNullOrEmpty()) return "Вышки не найдены"

        list.forEachIndexed { i, info ->
            val reg = if (info.isRegistered) "[АКТИВ]" else "[СОСЕД]"
            sb.append("${i+1}. $reg ")
            when (info) {
                is CellInfoLte -> sb.append("LTE | PCI: ${info.cellIdentity.pci} | RSRP: ${info.cellSignalStrength.rsrp}dBm")
                is CellInfoNr -> sb.append("5G NR | PCI: ${(info.cellIdentity as CellIdentityNr).pci} | RSRP: ${(info.cellSignalStrength as CellSignalStrengthNr).ssRsrp}")
                is CellInfoGsm -> sb.append("GSM | CID: ${info.cellIdentity.cid} | Signal: ${info.cellSignalStrength.dbm}")
                else -> sb.append("Другой тип сети")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun sendToBackend(payload: String) {
        scope.launch {
            try {
                if (zmqSocket == null || lastConnectedIp != currentServerIp || lastConnectedPort != currentServerPort) {
                    Log.w(TAG, "⚠️ Socket not ready, reinitializing...")
                    initZmqSocket()
                    delay(200)
                }

                zmqSocket?.let { sock ->
                    Log.d(TAG, "📤 Sending ${payload.length} bytes to $currentServerIp:$currentServerPort")
                    sock.send(payload.toByteArray(ZMQ.CHARSET), 0)
                    Log.d(TAG, "✅ Packet sent successfully")
                } ?: run {
                    Log.e(TAG, "❌ ZMQ socket is null!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ ZMQ send error: ${e.message}")
                e.printStackTrace()
                zmqSocket?.close()
                zmqSocket = null
            }
        }
    }

    private fun writeToPhoneLog(line: String) {
        try {
            FileOutputStream(File(filesDir, "location_history.json"), true).use {
                it.write((line + "\n").toByteArray())
            }
            Log.d(TAG, "💾 Written to local log")
        } catch (e: Exception) {
            Log.e(TAG, "❌ File write error: ${e.message}")
        }
    }

    private fun sendBroadcastUpdate(txt: String, json: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(BROADCAST_ACTION).apply {
            putExtra("Status", "Обновлено: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}")
            putExtra("CellInfo", txt)
            putExtra("LastJson", json)
        })
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "🛑 Service onDestroy()")
        periodicRunnable?.let { handler.removeCallbacks(it) }
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }

        try {
            zmqSocket?.close()
            zmqContext.close()
            Log.d(TAG, "✅ ZMQ resources closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ZMQ: ${e.message}")
        }

        scope.cancel()
        super.onDestroy()
    }
}