package com.example.bluetoothmicmonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class MonitorService : Service() {
    companion object {
        const val ACTION_START = "monitor.START"
        const val ACTION_STOP = "monitor.STOP"
        const val ACTION_UPDATE = "monitor.UPDATE"
        const val EXTRA_GAIN = "gain"
        const val EXTRA_MUTED = "muted"
        private const val CHANNEL = "mic_monitor"
        @Volatile var running = false
            private set
    }

    private val active = AtomicBoolean(false)
    @Volatile private var gain = 1f
    @Volatile private var muted = false
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Microphone monitoring", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { shutdown(); stopSelf(); return START_NOT_STICKY }
            ACTION_UPDATE -> { updateSettings(intent); return START_NOT_STICKY }
            ACTION_START -> {
                updateSettings(intent)
                if (!active.compareAndSet(false, true)) return START_NOT_STICKY
                try {
                    if (Build.VERSION.SDK_INT >= 29) {
                        startForeground(1, notification("Monitoring microphone"), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                    } else startForeground(1, notification("Monitoring microphone"))
                } catch (e: Exception) {
                    active.set(false); stopSelf(); return START_NOT_STICKY
                }
                running = true
                worker = thread(name = "AudioMonitor", isDaemon = true) { audioLoop() }
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun updateSettings(intent: Intent) {
        gain = intent.getFloatExtra(EXTRA_GAIN, gain).coerceIn(0f, 2f)
        muted = intent.getBooleanExtra(EXTRA_MUTED, muted)
    }

    private fun notification(message: String): Notification {
        val stopIntent = Intent(this, MonitorService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val openPending = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Bluetooth Mic Monitor")
            .setContentText(message)
            .setContentIntent(openPending)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop", stopPending).build())
            .build()
    }

    private fun audioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val sampleRate = 48000
        val inputMin = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val outputMin = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (inputMin <= 0 || outputMin <= 0) { finishWorker(); return }
        val chunkSamples = 480 // 10 ms chunks; Bluetooth buffering remains device-dependent
        val recordBufferBytes = maxOf(inputMin, chunkSamples * 4)
        val playbackBufferBytes = maxOf(outputMin, chunkSamples * 4)
        var recorder: AudioRecord? = null
        var player: AudioTrack? = null
        try {
            recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, recordBufferBytes)
            player = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(playbackBufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            if (recorder.state != AudioRecord.STATE_INITIALIZED || player.state != AudioTrack.STATE_INITIALIZED) return
            val samples = ShortArray(chunkSamples)
            recorder.startRecording()
            player.play()
            while (active.get() && !Thread.currentThread().isInterrupted) {
                val count = recorder.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                if (count <= 0) break
                val multiplier = if (muted) 0f else gain
                for (i in 0 until count) {
                    samples[i] = (samples[i] * multiplier).toInt().coerceIn(-32768, 32767).toShort()
                }
                var offset = 0
                while (offset < count && active.get()) {
                    val written = player.write(samples, offset, count - offset, AudioTrack.WRITE_BLOCKING)
                    if (written <= 0) break
                    offset += written
                }
                if (offset < count) break
            }
        } catch (_: SecurityException) {
        } catch (_: IllegalStateException) {
        } finally {
            try { recorder?.stop() } catch (_: Exception) {}
            try { player?.pause(); player?.flush(); player?.stop() } catch (_: Exception) {}
            recorder?.release()
            player?.release()
            finishWorker()
        }
    }

    private fun finishWorker() {
        active.set(false)
        running = false
        stopSelf()
    }

    private fun shutdown() {
        active.set(false)
        running = false
        worker?.interrupt()
        worker = null
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }
}
