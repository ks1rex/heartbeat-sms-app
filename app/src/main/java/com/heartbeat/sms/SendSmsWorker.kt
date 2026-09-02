package com.heartbeat.sms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SendSmsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        SmsSender.sendHeartbeatBlocking(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Result.failure()
    }
}
