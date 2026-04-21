package com.example.android_project

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class CellLocationServiceActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var cellInfoText: TextView
    private lateinit var lastJsonText: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnShowLastJson: Button
    private lateinit var etIp: EditText
    private lateinit var etPort: EditText

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            statusText.text = intent?.getStringExtra("Status") ?: "Сервис остановлен"
            cellInfoText.text = intent?.getStringExtra("CellInfo") ?: "Нет данных о вышках"
            
            val lastJson = intent?.getStringExtra("LastJson")
            if (lastJson != null) {
                lastJsonText.text = lastJson
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cell_location_service)

        statusText = findViewById(R.id.tv_status)
        cellInfoText = findViewById(R.id.tv_cell_info)
        lastJsonText = findViewById(R.id.tv_last_json)
        btnStart = findViewById(R.id.btn_start_service)
        btnStop = findViewById(R.id.btn_stop_service)
        btnShowLastJson = findViewById(R.id.btn_show_last_json)
        etIp = findViewById(R.id.et_server_ip)
        etPort = findViewById(R.id.et_server_port)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver, IntentFilter(CellLocationService.BROADCAST_ACTION)
        )

        statusText.text = "Сервис остановлен"
        statusText.setTextColor(getColor(android.R.color.holo_red_dark))
        cellInfoText.text = "Запустите сервис для получения данных о сетях"
        
        btnShowLastJson.setOnClickListener {
            // Кнопка может просто прокручивать к JSON или обновлять его принудительно, 
            // но сейчас мы обновляем его в реальном времени через broadcast.
        }
    }

    override fun onResume() {
        super.onResume()

        btnStart.setOnClickListener {
            val ip = etIp.text.toString()
            val port = etPort.text.toString()

            val intent = Intent(this, CellLocationService::class.java).apply {
                putExtra("SERVER_IP", ip)
                putExtra("SERVER_PORT", port)
            }

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
            lastJsonText.text = "Сервис остановлен"
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }
}