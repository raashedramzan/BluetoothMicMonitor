package com.example.bluetoothmicmonitor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var mute: Button
    private var gain = 1.0f
    private var muted = false
    private val requestCode = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 52, 36, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        fun text(s: String, size: Float = 16f): TextView = TextView(this).apply {
            text = s; textSize = size; setPadding(0, 12, 0, 12)
            root.addView(this)
        }
        fun button(s: String, onClick: () -> Unit): Button = Button(this).apply {
            text = s; root.addView(this); setOnClickListener { onClick() }
        }
        text("Bluetooth Mic Monitor", 25f)
        status = text("Stopped")
        text("Pair and connect your Bluetooth speaker in Android Settings before starting. Set media output to that speaker.")
        button("Open Bluetooth settings") {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        button("Start monitoring") { startWithPermission() }
        button("Stop monitoring") { sendService(MonitorService.ACTION_STOP) }
        mute = button("Mute microphone") {
            muted = !muted
            mute.text = if (muted) "Unmute microphone" else "Mute microphone"
            sendService(MonitorService.ACTION_UPDATE)
        }
        text("Microphone gain: 0% to 200% (start low)")
        val gainValue = text("100%")
        root.addView(SeekBar(this).apply {
            max = 200; progress = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    gain = progress / 100f
                    gainValue.text = "$progress%"
                    if (fromUser) sendService(MonitorService.ACTION_UPDATE)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        })
        text("WARNING: Keep the speaker away from the phone microphone. Bluetooth playback can be delayed and may produce loud feedback.")
        setContentView(root)
    }

    private fun startWithPermission() {
        val needed = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), requestCode)
        else startMonitoring()
    }

    private fun startMonitoring() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        if (!am.isBluetoothA2dpOn) {
            Toast.makeText(this, "Check that your Bluetooth speaker is selected as media output", Toast.LENGTH_LONG).show()
        }
        val intent = serviceIntent(MonitorService.ACTION_START)
        try {
            startForegroundService(intent)
            status.text = "Monitoring requested. Check the notification for status."
        } catch (e: Exception) {
            status.text = "Could not start: ${e.message}"
        }
    }

    private fun serviceIntent(action: String) = Intent(this, MonitorService::class.java).apply {
        this.action = action
        putExtra(MonitorService.EXTRA_GAIN, gain)
        putExtra(MonitorService.EXTRA_MUTED, muted)
    }
    private fun sendService(action: String) {
        if (action == MonitorService.ACTION_UPDATE && !MonitorService.running) return
        if (action == MonitorService.ACTION_STOP && !MonitorService.running) {
            status.text = "Stopped"; return
        }
        startService(serviceIntent(action))
        if (action == MonitorService.ACTION_STOP) status.text = "Stopped"
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) status.text = if (MonitorService.running) "Monitoring active" else "Stopped"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startMonitoring()
            else Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_LONG).show()
        }
    }
}
