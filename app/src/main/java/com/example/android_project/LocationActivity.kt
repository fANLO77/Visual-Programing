package com.example.android_project

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var textLatitude: TextView
    private lateinit var textLongitude: TextView
    private lateinit var textAltitude: TextView
    private lateinit var textTime: TextView
    private val PERMISSION_REQUEST_CODE = 102
    private val logTag = "Локация"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        textLatitude = findViewById(R.id.text_latitude)
        textLongitude = findViewById(R.id.text_longitude)
        textAltitude = findViewById(R.id.text_altitude)
        textTime = findViewById(R.id.text_time)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d(logTag, "Обновление локации: lat=$${location.latitude}, lon= $${location.longitude}")

                    updateLocationUI(location)

                    writeLocationToJson(location)
                } ?: run {
                    Log.w(logTag, "Локация null")
                }
            }
        }

        checkPermissions()
    }
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            getLastLocation()
            requestLocationUpdates()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(logTag, "Разрешения даны")
                getLastLocation()
                requestLocationUpdates()
            } else {
                Log.e(logTag, "Разрешения отклонены")
                textLatitude.text = "Разрешения не даны"
            }
        }
    }
    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                Log.d(logTag, "Последняя локация: lat=$$ {it.latitude}, lon= $${it.longitude}")
                updateLocationUI(it)
                writeLocationToJson(it)
            } ?: run {
                Log.w(logTag, "Последняя локация null")
                textLatitude.text = "Локация не доступна"
            }
        }.addOnFailureListener { e ->
            Log.e(logTag, "Ошибка получения локации: ${e.message}")
            textLatitude.text = "Ошибка: ${e.message}"
        }
    }
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        Log.d(logTag, "Обновления локации запрошены")
    }
    private fun updateLocationUI(location: Location) {
        textLatitude.text = "Широта: ${location.latitude}"
        textLongitude.text = "Долгота: ${location.longitude}"
        textAltitude.text = if (location.hasAltitude()) "Высота: ${location.altitude}" else "Высота: N/A"
        val time = location.time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        textTime.text = "Время: ${dateFormat.format(Date(time))}"
    }
    private fun writeLocationToJson(location: Location) {
        val gson = Gson()
        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "altitude" to if (location.hasAltitude()) location.altitude else null,
            "time" to location.time
        )
        val json = gson.toJson(data) + "\n"
        val file = File(filesDir, "location_data.json")
        try {
            FileOutputStream(file, true).use { outputStream ->
                outputStream.write(json.toByteArray())
            }
            Log.d(logTag, "Данные записаны в JSON: $json")
        } catch (e: Exception) {
            Log.e(logTag, "Ошибка записи в файл: ${e.message}")
        }
    }
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(logTag, "Обновления локации остановлены")
    }
}