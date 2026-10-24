package com.example.fitnesstimer

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var timeText: TextView
    private lateinit var minutePicker: NumberPicker
    private lateinit var secondPicker: NumberPicker
    private lateinit var pickerRow: View
    private lateinit var startButton: Button
    private lateinit var resetButton: Button

    private var timer: CountDownTimer? = null
    private var player: MediaPlayer? = null
    private var remainingMillis = 0L
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeText = findViewById(R.id.timeText)
        minutePicker = findViewById(R.id.minutePicker)
        secondPicker = findViewById(R.id.secondPicker)
        pickerRow = findViewById(R.id.pickerRow)
        startButton = findViewById(R.id.startButton)
        resetButton = findViewById(R.id.resetButton)

        val twoDigits = NumberPicker.Formatter { "%02d".format(it) }
        minutePicker.apply { minValue = 0; maxValue = 99; setFormatter(twoDigits) }
        secondPicker.apply { minValue = 0; maxValue = 59; setFormatter(twoDigits) }

        val onPick = NumberPicker.OnValueChangeListener { _, _, _ ->
            remainingMillis = pickedMillis()
            showTime(remainingMillis)
        }
        minutePicker.setOnValueChangedListener(onPick)
        secondPicker.setOnValueChangedListener(onPick)

        startButton.setOnClickListener { if (running) pause() else start() }
        resetButton.setOnClickListener { reset() }

        showTime(0)
    }

    private fun pickedMillis() = (minutePicker.value * 60L + secondPicker.value) * 1000L

    private fun start() {
        if (remainingMillis <= 0) remainingMillis = pickedMillis()
        if (remainingMillis <= 0) return

        running = true
        pickerRow.visibility = View.INVISIBLE
        startButton.setText(R.string.pause)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        timer = object : CountDownTimer(remainingMillis, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                showTime(millisUntilFinished)
            }

            override fun onFinish() {
                remainingMillis = 0
                showTime(0)
                stopRunning()
                startButton.setText(R.string.start)
                pickerRow.visibility = View.VISIBLE
                playBell()
            }
        }.start()
    }

    private fun pause() {
        timer?.cancel()
        stopRunning()
        startButton.setText(R.string.resume)
    }

    private fun reset() {
        timer?.cancel()
        stopRunning()
        stopBell()
        remainingMillis = pickedMillis()
        showTime(remainingMillis)
        startButton.setText(R.string.start)
        pickerRow.visibility = View.VISIBLE
    }

    private fun stopRunning() {
        running = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun showTime(millis: Long) {
        // 올림 처리: 남은 0.3초는 00:01로 보이고, 정확히 끝날 때 00:00이 된다
        val totalSeconds = (millis + 999) / 1000
        timeText.text = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    private fun playBell() {
        stopBell()
        player = MediaPlayer.create(this, R.raw.boxingbell)?.apply {
            setOnCompletionListener { stopBell() }
            start()
        }
    }

    private fun stopBell() {
        player?.release()
        player = null
    }

    override fun onDestroy() {
        timer?.cancel()
        stopBell()
        super.onDestroy()
    }
}
