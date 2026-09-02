package com.heartbeat.sms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var numbersEdit: EditText
    private lateinit var delayEdit: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        numbersEdit = findViewById(R.id.numbersEdit)
        delayEdit = findViewById(R.id.delayEdit)
        statusText = findViewById(R.id.statusText)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val sendButton = findViewById<Button>(R.id.sendButton)

        numbersEdit.setText(SmsSender.getNumbers(this).joinToString("\n"))
        delayEdit.setText(SmsSender.getDelayMs(this).toString())
        updateStatus()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
        }

        saveButton.setOnClickListener {
            SmsSender.saveNumbers(this, numbersEdit.text.toString())
            saveDelay()
            Toast.makeText(this, "Список сохранён", Toast.LENGTH_SHORT).show()
        }

        sendButton.setOnClickListener {
            SmsSender.saveNumbers(this, numbersEdit.text.toString())
            saveDelay()
            sendButton.isEnabled = false
            statusText.text = "Отправка..."
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    SmsSender.sendHeartbeatBlocking(this@MainActivity) { progress ->
                        runOnUiThread { statusText.text = progress }
                    }
                }
                sendButton.isEnabled = true
                updateStatus()
                Toast.makeText(this@MainActivity, "Рассылка завершена", Toast.LENGTH_SHORT).show()
            }
        }

        schedulePeriodicWork()
    }

    private fun saveDelay() {
        val delayMs = delayEdit.text.toString().toLongOrNull() ?: SmsSender.getDelayMs(this)
        SmsSender.saveDelayMs(this, delayMs.coerceAtLeast(500L))
    }

    private fun schedulePeriodicWork() {
        val request = PeriodicWorkRequestBuilder<SendSmsWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_heartbeat",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun updateStatus() {
        val last = SmsSender.lastSent(this)
        statusText.text = if (last == 0L) {
            "Ещё не отправлялось"
        } else {
            "Последняя отправка: ${SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(last))}"
        }
    }
}
