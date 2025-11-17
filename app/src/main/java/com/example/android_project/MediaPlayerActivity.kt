package com.example.android_project
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.TimeUnit
class MediaPlayerActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBarProgress: SeekBar
    private lateinit var buttonPlayPause: Button
    private lateinit var seekBarVolume: SeekBar
    private lateinit var textCurrentTime: TextView
    private lateinit var textTotalTime: TextView
    private lateinit var trackListView: ListView
    private lateinit var audioManager: AudioManager
    private lateinit var buttonPrev: Button
    private lateinit var buttonNext: Button
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBar = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPlaying) {
                seekBarProgress.progress = mediaPlayer.currentPosition
                textCurrentTime.text = formatTime(mediaPlayer.currentPosition)
                handler.postDelayed(this, 1000)
            }
        }
    }
    private var currentTrackIndex = 0
    private val trackList = mutableListOf<File>()
    private val logTag = "МедиаПлеер"
    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        initViews()
        initMediaPlayer()
        checkPermissions()
    }
    private fun initViews() {
        seekBarProgress = findViewById(R.id.seekBar)
        buttonPlayPause = findViewById(R.id.playPauseButton)
        seekBarVolume = findViewById(R.id.volumeSeekBar)
        textCurrentTime = findViewById(R.id.currentTimeText)
        textTotalTime = findViewById(R.id.totalTimeText)
        trackListView = findViewById(R.id.trackListView)
        buttonPrev = findViewById(R.id.prevButton)
        buttonNext = findViewById(R.id.nextButton)
    }
    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnCompletionListener {
                Log.d(logTag, "Трек завершён")
                buttonPlayPause.text = "Воспроизвести"
                seekBarProgress.progress = 0
                if (currentTrackIndex < trackList.size - 1) {
                    playTrack(currentTrackIndex + 1)
                } else {
                    Toast.makeText(this@MediaPlayerActivity, "Плейлист завершён", Toast.LENGTH_SHORT).show()
                }
            }
            buttonPrev.setOnClickListener {
                if (currentTrackIndex > 0) {
                    playTrack(currentTrackIndex - 1)
                    Log.d(logTag, "Переход к предыдущему: ${trackList[currentTrackIndex].name}")
                } else {
                    Toast.makeText(this@MediaPlayerActivity, "Это первый трек", Toast.LENGTH_SHORT).show()
                    Log.d(logTag, "Попытка перейти до первого трека")
                }
            }
            buttonNext.setOnClickListener {
                if (currentTrackIndex < trackList.size - 1) {
                    playTrack(currentTrackIndex + 1)
                    Log.d(logTag, "Переход к следующему: ${trackList[currentTrackIndex].name}")
                } else {
                    Toast.makeText(this@MediaPlayerActivity, "Это последний трек", Toast.LENGTH_SHORT).show()
                    Log.d(logTag, "Попытка перейти после последнего трека")
                }
            }
        }
        seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    Log.d(logTag, "Перемотка на: $progress мс")
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        seekBarVolume.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        seekBarVolume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                    Log.d(logTag, "Громкость: $progress")
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        buttonPlayPause.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                buttonPlayPause.text = "Play"
                handler.removeCallbacks(updateSeekBar)
                Log.d(logTag, "Трек на паузе")
            } else {
                if (::mediaPlayer.isInitialized && trackList.isNotEmpty()) {
                    mediaPlayer.start()
                    buttonPlayPause.text = "Pause"
                    handler.post(updateSeekBar)
                    Log.d(logTag, "Трек запущен")
                } else {
                    Toast.makeText(this@MediaPlayerActivity, "Трек не выбран", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this@MediaPlayerActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this@MediaPlayerActivity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this@MediaPlayerActivity , permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            loadMusicFiles()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadMusicFiles()
            } else {
                Toast.makeText(this@MediaPlayerActivity, "Разрешите доступ к музыке", Toast.LENGTH_LONG).show()
                Log.e(logTag, "Разрешения отклонены")
            }
        }
    }
    private fun BubbleSort(tracks: MutableList<File>) {
        val n = tracks.size
        var swapped: Boolean
        for (i in 0 until n - 1) {
            swapped = false
            for (j in 0 until n - i - 1) {
                if (tracks[j].name.compareTo(tracks[j + 1].name, ignoreCase = true) > 0) {
                    val temp = tracks[j]
                    tracks[j] = tracks[j + 1]
                    tracks[j + 1] = temp
                    swapped = true
                    Log.d(logTag, "Сортировка: ${tracks[j + 1].name} ↔ ${tracks[j].name}")
                }
            }
            if (!swapped) break
        }
        Log.d(logTag, "Сортировка завершена. Треков: ${tracks.size}")
    }
    private fun updateTrackButtons() {
        buttonPrev.isEnabled = currentTrackIndex > 0
        buttonNext.isEnabled = currentTrackIndex < trackList.size - 1
    }
    private fun loadMusicFiles() {
        val musicPath = "${getExternalFilesDir(Environment.DIRECTORY_MUSIC)}"
        Log.d(logTag, "Путь к музыке: $musicPath")
        val directory = File(musicPath)
        if (!directory.exists() || !directory.isDirectory) {
            Toast.makeText(this@MediaPlayerActivity, "Папка /Music не найдена", Toast.LENGTH_LONG).show()
            Log.e(logTag, "Папка не существует")
            return
        }
        val files = directory.listFiles()?.filter { file ->
            file.isFile && (file.name.endsWith(".mp3", ignoreCase = true) || file.name.endsWith(".wav", ignoreCase = true))
        }
        if (!files.isNullOrEmpty()) {
            trackList.clear()
            trackList.addAll(files)
            BubbleSort(trackList)
            Log.d(logTag, "Загружено и отсортировано треков: ${trackList.size}")
            setupListView()
        } else {
            Toast.makeText(this@MediaPlayerActivity, "Нет MP3/WAV в /Music", Toast.LENGTH_LONG).show()
            Log.w(logTag, "Папка пуста")
        }
        Log.d("МУЗЫКА", "Путь: $musicPath")
        Log.d("МУЗЫКА", "Папка существует: ${directory.exists()}")
        Log.d("МУЗЫКА", "Это директория: ${directory.isDirectory}")
        Log.d("МУЗЫКА", "Файлы в папке: ${directory.listFiles()?.map { it.name }.toString()}")
    }
    private fun setupListView() {
        val trackNames = trackList.map { it.name }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            trackNames
        )
        trackListView.adapter = adapter
        trackListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            playTrack(position)
            updateTrackButtons()
        }
    }
    private fun playTrack(position: Int) {
        if (position < 0 || position >= trackList.size) return
        currentTrackIndex = position
        val track = trackList[position]
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(this, Uri.fromFile(track))
            mediaPlayer.prepare()
            mediaPlayer.start()
            seekBarProgress.max = mediaPlayer.duration
            textTotalTime.text = formatTime(mediaPlayer.duration)
            textCurrentTime.text = "00:00"
            buttonPlayPause.text = "Pause"
            handler.post(updateSeekBar)
            Toast.makeText(this, "Играет: ${track.name}", Toast.LENGTH_SHORT).show()
            Log.d(logTag, "Воспроизведение: ${track.name}")
            updateTrackButtons()
        } catch (e: Exception) {
            Log.e(logTag, "Ошибка воспроизведения: ${e.message}")
            Toast.makeText(this@MediaPlayerActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun formatTime(millis: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis.toLong()) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            buttonPlayPause.text = "Воспроизвести"
            handler.removeCallbacks(updateSeekBar)
            Log.d(logTag, "Приложение на паузе - трек остановлен")
        }
    }
    override fun onStop() {
        super.onStop()
        mediaPlayer.pause()
        handler.removeCallbacks(updateSeekBar)
        Log.d(logTag, "Приложение остановлено - трек на паузе")
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        handler.removeCallbacks(updateSeekBar)
        Log.d(logTag, "MediaPlayer освобождён")
    }
}