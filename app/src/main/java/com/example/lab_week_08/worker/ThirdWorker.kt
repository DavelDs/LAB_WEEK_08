package com.example.lab_week_08.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class ThirdWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        const val INPUT_DATA_ID = "ThirdWorker_ID"
    }

    override fun doWork(): Result {
        val id = inputData.getString(INPUT_DATA_ID) ?: "Unknown"
        Log.d("ThirdWorker", "ThirdWorker with ID: $id is running")

        // Simulasi kerja 3 detik biar terasa prosesnya
        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
            return Result.failure()
        }

        Log.d("ThirdWorker", "ThirdWorker with ID: $id finished successfully!")
        return Result.success()
    }
}
