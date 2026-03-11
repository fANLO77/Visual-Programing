package com.example.android_project

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class CellLocationServiceActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var cellInfoText: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            statusText.text = intent?.getStringExtra("Status") ?: "Сервис остановлен"
            cellInfoText.text = intent?.getStringExtra("CellInfo") ?: "Нет данных о вышках"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cell_location_service)

        statusText = findViewById(R.id.tv_status)
        cellInfoText = findViewById(R.id.tv_cell_info)
        btnStart = findViewById(R.id.btn_start_service)
        btnStop = findViewById(R.id.btn_stop_service)

        // Регистрация receiver, как в примере
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver, IntentFilter(CellLocationService.BROADCAST_ACTION)
        )

        // Начальное состояние
        statusText.text = "Сервис остановлен"
        statusText.setTextColor(getColor(android.R.color.holo_red_dark))
        cellInfoText.text = "Запустите сервис для получения данных о сетях"
    }

    override fun onResume() {
        super.onResume()

        btnStart.setOnClickListener {
            val intent = Intent(this, CellLocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            statusText.text = "Сервис запущен"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, CellLocationService::class.java))
            statusText.text = "Сервис остановлен"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
            cellInfoText.text = "Сервис остановлен\nДанные о вышках сброшены"
        }
    }

    override fun onDestroy() {
        // Unregister receiver, как в примере (хотя в примере unregister в onDestroy нет, но хорошая практика)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }
}