package com.example.android_project

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class LocationActivity : AppCompatActivity() {
    // UI элементы
    private lateinit var textLatitude: TextView
    private lateinit var textLongitude: TextView
    private lateinit var textAltitude: TextView
    private lateinit var textTime: TextView
    private lateinit var textStatus: TextView // Добавь это в XML для статуса сети

    // Локация
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // ZMQ и Потоки
    private val zmqContext = ZContext()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // НАСТРОЙКИ (Проверь свой IP через hostname -I в WSL)
    private val SERVER_IP = "172.31.53.226"
    private val SERVER_PORT = "5555"
    private val PERMISSION_REQUEST_CODE = 102
    private val TAG = "LocationLog"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        // Инициализация View
        textLatitude = findViewById(R.id.text_latitude)
        textLongitude = findViewById(R.id.text_longitude)
        textAltitude = findViewById(R.id.text_altitude)
        textTime = findViewById(R.id.text_time)
        // Если в XML нет text_status, закомментируй строку ниже или добавь TextView
        textStatus = findViewById(R.id.text_status)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Callback получения координат
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    processNewLocation(location)
                }
            }
        }

        checkPermissions()
    }

    private fun processNewLocation(location: Location) {
        // 1. Обновляем экран
        updateLocationUI(location)

        // 2. Пишем в локальный файл (для истории)
        writeLocationToLocalJson(location)

        // 3. Отправляем на C++ сервер (ZMQ)
        sendLocationToCppServer(location)
    }

    private fun sendLocationToCppServer(location: Location) {
        scope.launch {
            var socket: ZMQ.Socket? = null
            try {
                // Создаем сокет типа REQ (Запрос)
                socket = zmqContext.createSocket(SocketType.REQ)

                // Устанавливаем таймауты, чтобы REQ не блокировался навсегда
                socket.receiveTimeOut = 3000
                socket.sendTimeOut = 3000

                val address = "tcp://$SERVER_IP:$SERVER_PORT"
                socket.connect(address)

                // Формируем JSON пакет (соответствует структуре сервера)
                val data = mapOf(
                    "lat" to location.latitude,
                    "lon" to location.longitude,
                    "alt" to if (location.hasAltitude()) location.altitude else 0.0,
                    "time" to SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                )
                val jsonPayload = Gson().toJson(data)

                // Попытка отправки
                val isSent = socket.send(jsonPayload.toByteArray(ZMQ.CHARSET), 0)

                if (isSent) {
                    // Ждем ответ от REP сокета сервера
                    val response = socket.recvStr(0)
                    if (response != null) {
                        updateStatus("Сервер: $response")
                        Log.d(TAG, "Успешно отправлено: $jsonPayload")
                    } else {
                        updateStatus("Сервер не ответил (Timeout)")
                    }
                } else {
                    updateStatus("Ошибка отправки")
                }

            } catch (e: Exception) {
                // Тот самый try-catch для обработки разрыва соединения по ТЗ
                Log.e(TAG, "Ошибка ZMQ: ${e.message}")
                updateStatus("Разрыв соединения: реконнект...")
            } finally {
                // Закрываем сокет после каждой итерации для чистого реконнекта
                socket?.close()
            }
        }
    }

    private fun updateLocationUI(location: Location) {
        runOnUiThread {
            textLatitude.text = "Широта: ${location.latitude}"
            textLongitude.text = "Долгота: ${location.longitude}"
            textAltitude.text = "Высота: ${if (location.hasAltitude()) location.altitude else "N/A"}"
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            textTime.text = "Время: ${sdf.format(Date(location.time))}"
        }
    }

    private fun updateStatus(msg: String) {
        runOnUiThread {
            // Если добавишь TextView для статуса в XML
            textStatus.text = "ZMQ: $msg"
        }
    }

    private fun writeLocationToLocalJson(location: Location) {
        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "altitude" to if (location.hasAltitude()) location.altitude else null,
            "time" to location.time
        )
        val json = Gson().toJson(data) + "\n"
        try {
            val file = File(filesDir, "location_history.json")
            FileOutputStream(file, true).use { it.write(json.toByteArray()) }
        } catch (e: Exception) {
            Log.e(TAG, "Файл не записан: ${e.message}")
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            requestLocationUpdates()
        }
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000 // Каждые 5 секунд
            fastestInterval = 2000
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            updateStatus("Поиск GPS...")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        scope.cancel() // Останавливаем корутины
        zmqContext.destroy() // Закрываем контекст ZMQ
    }
}