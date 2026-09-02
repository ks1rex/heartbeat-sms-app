package com.heartbeat.sms

import android.content.Context
import android.telephony.SmsManager

fun parseNumbers(raw: String): List<String> =
    raw.lines().map { it.trim() }.filter { it.isNotEmpty() }

object SmsSender {
    private const val PREFS = "heartbeat_prefs"
    private const val KEY_NUMBERS = "numbers"
    private const val KEY_LAST_SENT = "last_sent"
    private const val DELAY_MS = 2500L // ponytail: fixed pacing, add backoff-on-failure if carrier throttling shows up

    fun getNumbers(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_NUMBERS, "") ?: ""
        return parseNumbers(raw)
    }

    fun saveNumbers(context: Context, text: String) {
        prefs(context).edit().putString(KEY_NUMBERS, text).apply()
    }

    fun lastSent(context: Context): Long = prefs(context).getLong(KEY_LAST_SENT, 0L)

    /** Blocking — call from a background thread only. */
    fun sendHeartbeatBlocking(context: Context, onProgress: (String) -> Unit = {}) {
        val numbers = getNumbers(context)
        val smsManager = context.getSystemService(SmsManager::class.java)
        val text = "heartbeat ${System.currentTimeMillis()}"
        for (number in numbers) {
            try {
                smsManager.sendTextMessage(number, null, text, null, null)
                onProgress("OK: $number")
            } catch (e: Exception) {
                onProgress("FAIL: $number (${e.message})")
            }
            Thread.sleep(DELAY_MS)
        }
        prefs(context).edit().putLong(KEY_LAST_SENT, System.currentTimeMillis()).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
