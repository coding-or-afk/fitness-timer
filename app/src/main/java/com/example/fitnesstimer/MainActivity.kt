package com.example.fitnesstimer

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private enum class State { READY, RUNNING, PAUSED, DONE }

    private lateinit var timeText: TextView
    private lateinit var statusText: TextView
    private lateinit var minutePicker: NumberPicker
    private lateinit var secondPicker: NumberPicker
    private lateinit var pickerRow: View
    private lateinit var startButton: Button
    private lateinit var resetButton: Button
    private lateinit var themeButton: ImageButton

    private var timer: CountDownTimer? = null
    private var player: MediaPlayer? = null
    private var remainingMillis = 0L
    private var state = State.READY

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            getPreferences(MODE_PRIVATE).getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeText = findViewById(R.id.timeText)
        statusText = findViewById(R.id.statusText)
        minutePicker = findViewById(R.id.minutePicker)
        secondPicker = findViewById(R.id.secondPicker)
        pickerRow = findViewById(R.id.pickerRow)
        startButton = findViewById(R.id.startButton)
        resetButton = findViewById(R.id.resetButton)
        themeButton = findViewById(R.id.themeButton)

        val twoDigits = NumberPicker.Formatter { "%02d".format(it) }
        minutePicker.apply { minValue = 0; maxValue = 99; setFormatter(twoDigits) }
        secondPicker.apply { minValue = 0; maxValue = 59; setFormatter(twoDigits) }

        val onPick = NumberPicker.OnValueChangeListener { _, _, _ ->
            remainingMillis = pickedMillis()
            showTime(remainingMillis)
            setState(State.READY)
        }
        minutePicker.setOnValueChangedListener(onPick)
        secondPicker.setOnValueChangedListener(onPick)

        startButton.setOnClickListener { if (state == State.RUNNING) pause() else start() }
        resetButton.setOnClickListener { reset() }
        themeButton.setOnClickListener { toggleNightMode() }

        if (savedInstanceState != null) restore(savedInstanceState) else showTime(0)
    }

    private fun pickedMillis() = (minutePicker.value * 60L + secondPicker.value) * 1000L

    private fun start() {
        if (remainingMillis <= 0) {
            remainingMillis = pickedMillis()
        }
        if (remainingMillis <= 0) return

        stopBell()
        requestNotificationPermission()
        setState(State.RUNNING)

        timer = object : CountDownTimer(remainingMillis, 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                showTime(millisUntilFinished)
            }

            override fun onFinish() {
                remainingMillis = 0
                showTime(0)
                setState(State.DONE)
                TimerNotificationService.stop(this@MainActivity)
                playBell()
            }
        }.start()
    }

    private fun pause() {
        timer?.cancel()
        setState(State.PAUSED)
    }

    private fun reset() {
        timer?.cancel()
        stopBell()
        remainingMillis = pickedMillis()
        showTime(remainingMillis)
        setState(State.READY)
    }

    private fun setState(newState: State) {
        state = newState
        startButton.setText(
            when (state) {
                State.RUNNING -> R.string.pause
                State.PAUSED -> R.string.resume
                else -> R.string.start
            }
        )
        statusText.setText(
            when (state) {
                State.READY -> R.string.status_ready
                State.RUNNING -> R.string.status_running
                State.PAUSED -> R.string.status_paused
                State.DONE -> R.string.status_done
            }
        )
        // 실행 중이거나 일시정지 중에는 시간을 바꿀 수 없다
        pickerRow.visibility =
            if (state == State.READY || state == State.DONE) View.VISIBLE else View.INVISIBLE
        if (state == State.RUNNING) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun showTime(millis: Long) {
        // 올림 처리: 남은 0.3초는 00:01로 보이고, 정확히 끝날 때 00:00이 된다
        val totalSeconds = (millis + 999) / 1000
        timeText.text = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    override fun onStart() {
        super.onStart()
        TimerNotificationService.stop(this)
    }

    override fun onStop() {
        super.onStop()
        // 다크 모드 전환으로 다시 만들어질 때는 알림을 띄우지 않는다
        if (state == State.RUNNING && !isChangingConfigurations) {
            TimerNotificationService.start(this, remainingMillis)
        }
    }

    private fun toggleNightMode() {
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val mode = if (isNight) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        getPreferences(MODE_PRIVATE).edit().putInt(KEY_NIGHT_MODE, mode).apply()
        // 액티비티가 다시 만들어지므로 타이머 상태는 onSaveInstanceState로 넘긴다
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_STATE, state.name)
        outState.putInt(KEY_MINUTES, minutePicker.value)
        outState.putInt(KEY_SECONDS, secondPicker.value)
        outState.putLong(KEY_REMAINING, remainingMillis)
        outState.putLong(KEY_END_AT, SystemClock.elapsedRealtime() + remainingMillis)
    }

    private fun restore(saved: Bundle) {
        minutePicker.value = saved.getInt(KEY_MINUTES)
        secondPicker.value = saved.getInt(KEY_SECONDS)
        remainingMillis = saved.getLong(KEY_REMAINING)
        var savedState = State.valueOf(saved.getString(KEY_STATE) ?: State.READY.name)

        if (savedState == State.RUNNING) {
            // 저장 후 흐른 시간만큼 빼서 이어서 진행한다
            remainingMillis = saved.getLong(KEY_END_AT) - SystemClock.elapsedRealtime()
            if (remainingMillis > 0) {
                showTime(remainingMillis)
                start()
                return
            }
            remainingMillis = 0
            savedState = State.DONE
        }
        showTime(remainingMillis)
        setState(savedState)
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
        if (!isChangingConfigurations) TimerNotificationService.stop(this)
        super.onDestroy()
    }

    private companion object {
        const val KEY_NIGHT_MODE = "night_mode"
        const val KEY_STATE = "state"
        const val KEY_MINUTES = "minutes"
        const val KEY_SECONDS = "seconds"
        const val KEY_REMAINING = "remaining"
        const val KEY_END_AT = "end_at"
    }
}
