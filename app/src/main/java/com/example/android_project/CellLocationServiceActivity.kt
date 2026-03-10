package com.example.android_project

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CellLocationServiceActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var cellInfoText: TextView   // новый TextView для вывода телефонии
    private val handler = Handler(Looper.getMainLooper())

    // Runnable для периодического обновления экрана
    private val updateRunnable = object : Runnable {
        override fun run() {
            // Обновляем статус сервиса
            statusText.text = if (CellLocationService.lastUpdateTime > 0) {
                "Сервис работает (последнее обновление: ${
                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(CellLocationService.lastUpdateTime))
                })"
            } else {
                "Сервис остановлен"
            }

            // Обновляем текст о вышках
            cellInfoText.text = CellLocationService.lastCellInfoText.ifEmpty {
                "Нет данных о вышках\n(запустите сервис и подождите 30–60 сек)"
            }

            // Повторяем каждые 5 секунд
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cell_location_service)

        statusText = findViewById(R.id.tv_status)
        cellInfoText = findViewById(R.id.tv_cell_info)  // ← должен быть в layout

        // Кнопка запуска
        findViewById<Button>(R.id.btn_start_service).setOnClickListener {
            val intent = Intent(this, CellLocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            statusText.text = "Сервис запущен"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
        }

        // Кнопка остановки
        findViewById<Button>(R.id.btn_stop_service).setOnClickListener {
            stopService(Intent(this, CellLocationService::class.java))
            statusText.text = "Сервис остановлен"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
            cellInfoText.text = "Сервис остановлен\nДанные о вышках сброшены"
        }

        // Начальное состояние
        statusText.text = "Сервис остановлен"
        statusText.setTextColor(getColor(android.R.color.holo_red_dark))
        cellInfoText.text = "Запустите сервис для получения данных о сетях"

        // Запускаем обновление экрана
        handler.post(updateRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        super.onDestroy()
    }
}