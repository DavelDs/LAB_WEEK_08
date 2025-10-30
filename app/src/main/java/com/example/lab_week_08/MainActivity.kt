package com.example.lab_week_08

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker
import android.os.Build
import android.content.pm.PackageManager
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.observe



class MainActivity : AppCompatActivity() {
    //Create an instance of a work manager
    //Work manager manages all your requests and workers
    //it also sets up the sequence for all your processes
    private val workManager = WorkManager.getInstance(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v,
                                                                             insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,
                systemBars.bottom)
            insets
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }



        //Create a constraint of which your workers are bound to.
        //Here the workers cannot execute the given process if
        //there's no internet connection
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val id = "001"
        //There are two types of work request:
        //OneTimeWorkRequest and PeriodicWorkRequest
        //OneTimeWorkRequest executes the request just once
        //PeriodicWorkRequest executed the request periodically
        //Create a one time work request that includes
        //all the constraints and inputs needed for the worker
        //This request is created for the FirstWorker class
        val firstRequest = OneTimeWorkRequest
            .Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker
                .INPUT_DATA_ID, id)
            ).build()
        //This request is created for the SecondWorker class
        val secondRequest = OneTimeWorkRequest
            .Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker
                .INPUT_DATA_ID, id)
            ).build()
        //Sets up the process sequence from the work manager instance
        //Here it starts with FirstWorker, then SecondWorker
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()
        //All that's left to do is getting the output
        //Here, we receive the output and displaying the result as a toast message
        //You may notice the keyword "LiveData" and "observe"
        //LiveData is a data holder class in Android Jetpack
        //that's used to make a more reactive application
        //the reactive of it comes from the observe keyword,
        //which observes any data changes and immediately update the app UI

        //Here we're observing the returned LiveData and getting the
        //state result of the worker (Can be SUCCEEDED, FAILED, or CANCELLED)
        //isFinished is used to check if the state is either SUCCEEDED or FAILED
        workManager.getWorkInfoByIdLiveData(firstRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                showResult("First process is done ✅")

                // === 2️⃣ Second Worker ===
                val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
                    .setConstraints(networkConstraints)
                    .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
                    .build()

                workManager.enqueue(secondRequest)

                workManager.getWorkInfoByIdLiveData(secondRequest.id).observe(this) { secondInfo ->
                    if (secondInfo != null && secondInfo.state.isFinished) {
                        showResult("Second process is done ✅")

                        // === 3️⃣ Jalankan NotificationService ===
                        launchNotificationService()

                        // === 4️⃣ Third Worker ===
                        val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
                            .setConstraints(networkConstraints)
                            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, id))
                            .build()

                        workManager.enqueue(thirdRequest)

                        workManager.getWorkInfoByIdLiveData(thirdRequest.id).observe(this) { thirdInfo ->
                            if (thirdInfo != null && thirdInfo.state.isFinished) {
                                showResult("Third process is done ✅")

                                // === 5️⃣ Jalankan SecondNotificationService ===
                                launchSecondNotificationService()
                            }
                        }
                    }
                }
            }
        }
    }
    //Build the data into the correct format before passing it to the worker as input
    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()
    //Show the result as toast
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    //Launch the NotificationService
    private fun launchNotificationService() {
        //Observe if the service process is done or not
        //If it is, show a toast with the channel ID in it
        NotificationService.trackingCompletion.observe(
            this) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }

        //Create an Intent to start the NotificationService
        //An ID of "001" is also passed as the notification channel ID
        val serviceIntent = Intent(this,
            NotificationService::class.java).apply {
            putExtra(EXTRA_ID, "001")
        }

        //Start the foreground service through the Service Intent
        ContextCompat.startForegroundService(this, serviceIntent)
    }
    //Launch the SecondNotificationService
    private fun launchSecondNotificationService() {
        // Observe if the service process is done or not
        SecondNotificationService.trackingCompletion.observe(this) { Id ->
            showResult("Process for Second Notification Channel ID $Id is done!")
        }

        // Create an Intent to start the SecondNotificationService
        val secondServiceIntent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(EXTRA_ID, "002") // Bisa pakai ID berbeda biar jelas
        }

        // Start the foreground service
        ContextCompat.startForegroundService(this, secondServiceIntent)
    }

    companion object{
        const val EXTRA_ID = "Id"
    }

}
