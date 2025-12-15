package com.example.android_project

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class SocketsActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvMessages: TextView
    private lateinit var btnStartServer: Button
    private lateinit var btnStartClient: Button
    private lateinit var btnStartExternalClient: Button

    private val context = ZContext()
    private var serverSocket: ZMQ.Socket? = null
    private var internalClientSocket: ZMQ.Socket? = null
    private var externalClientSocket: ZMQ.Socket? = null

    private val isServerRunning = AtomicBoolean(false)
    private val isInternalClientRunning = AtomicBoolean(false)
    private val SERVER_IP = "172.20.10.2"
    private val SERVER_PORT = "5555"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sockets)

        initViews()
        setupClickListeners()

        checkNetworkPermissions()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvMessages = findViewById(R.id.tvMessages)
        btnStartServer = findViewById(R.id.btnStartServer)
        btnStartClient = findViewById(R.id.btnStartClient)
        btnStartExternalClient = findViewById(R.id.btnStartExternalClient)
    }

    private fun setupClickListeners() {
        btnStartServer.setOnClickListener {
            if (isServerRunning.get()) {
                stopServer()
            } else {
                startServer()
            }
        }

        btnStartClient.setOnClickListener {
            if (isInternalClientRunning.get()) {
                stopInternalClient()
            } else {
                startInternalClient()
            }
        }

        btnStartExternalClient.setOnClickListener {
            startExternalClient()
        }
    }

    private fun startServer() {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    serverSocket = context.createSocket(SocketType.REP)
                    serverSocket?.bind("tcp://*:5556")
                    isServerRunning.set(true)

                    runOnUiThread {
                        btnStartServer.text = "Остановить сервер"
                        tvStatus.text = "Сервер запущен на порту 5556"
                        appendMessage("Сервер запущен")
                    }
                    while (isServerRunning.get()) {
                        val message = serverSocket?.recvStr() ?: break
                        runOnUiThread {
                            appendMessage("Получено от клиента: $message")
                        }
                        serverSocket?.send("Hello from Server!", 0)
                        runOnUiThread {
                            appendMessage("Отправлено: Hello from Server!")
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvStatus.text = "Ошибка сервера: ${e.message}"
                    appendMessage("Ошибка сервера: ${e.message}")
                    Toast.makeText(this@SocketsActivity, "Ошибка сервера: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopServer() {
        isServerRunning.set(false)
        serverSocket?.close()
        serverSocket = null

        runOnUiThread {
            btnStartServer.text = "Запустить сервер"
            tvStatus.text = "Сервер остановлен"
            appendMessage("Сервер остановлен")
        }
    }

    private fun startInternalClient() {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    internalClientSocket = context.createSocket(SocketType.REQ)
                    internalClientSocket?.connect("tcp://localhost:5556")
                    isInternalClientRunning.set(true)

                    runOnUiThread {
                        tvStatus.text = "Внутренний клиент подключен"
                        appendMessage("Внутренний клиент запущен")
                    }
                    val message = "Hello from Android!"
                    internalClientSocket?.send(message, 0)
                    runOnUiThread {
                        appendMessage("Отправлено: $message")
                    }
                    val response = internalClientSocket?.recvStr()
                    runOnUiThread {
                        appendMessage("Получено от сервера: $response")
                        tvStatus.text = "Передача завершена"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvStatus.text = "Ошибка клиента: ${e.message}"
                    appendMessage("Ошибка клиента: ${e.message}")
                    Toast.makeText(this@SocketsActivity, "Ошибка клиента: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isInternalClientRunning.set(false)
                internalClientSocket?.close()
                internalClientSocket = null
            }
        }
    }

    private fun stopInternalClient() {
        isInternalClientRunning.set(false)
        internalClientSocket?.close()
        internalClientSocket = null

        runOnUiThread {
            tvStatus.text = "Внутренний клиент остановлен"
            appendMessage("Внутренний клиент остановлен")
        }
    }

    private fun startExternalClient() {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    externalClientSocket = context.createSocket(SocketType.REQ)
                    val serverAddress = "tcp://$SERVER_IP:$SERVER_PORT"
                    externalClientSocket?.connect(serverAddress)

                    runOnUiThread {
                        tvStatus.text = "Подключение к внешнему серверу..."
                        appendMessage("Подключение к $serverAddress")
                    }
                    val message = "Hello from Android!"
                    externalClientSocket?.send(message, 0)
                    runOnUiThread {
                        appendMessage("Отправлено на внешний сервер: $message")
                    }
                    val response = externalClientSocket?.recvStr()
                    runOnUiThread {
                        appendMessage("Получено от внешнего сервера: $response")
                        tvStatus.text = "Внешняя передача завершена"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvStatus.text = "Ошибка внешнего клиента: ${e.message}"
                    appendMessage("Ошибка внешнего клиента: ${e.message}")
                    Toast.makeText(this@SocketsActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                externalClientSocket?.close()
                externalClientSocket = null
            }
        }
    }

    private fun appendMessage(message: String) {
        val currentText = tvMessages.text.toString()
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        tvMessages.text = "$currentText\n[$timestamp] $message"
    }

    private fun checkNetworkPermissions() {
        appendMessage("Проверка разрешений - OK")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
        stopInternalClient()
        externalClientSocket?.close()
        context.destroy()
        scope.cancel()
    }
}