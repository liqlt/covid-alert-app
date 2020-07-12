package app.covidshield.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import app.covidshield.extensions.log
import app.covidshield.receiver.worker.HeadlessJsTaskWorker
import app.covidshield.receiver.worker.HeadlessJsTaskWorker.Companion.shouldStartHeadlessJsTask
import app.covidshield.receiver.worker.StateUpdatedWorker
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient.EXTRA_TOKEN

class ExposureNotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        log("onReceive", mapOf("action" to action))

        if (action == Intent.ACTION_NEW_OUTGOING_CALL) {
            if (shouldStartHeadlessJsTask(context)) {
                startHeadlessJsTaskWorker(context, "")
            } else {
                log("Noop when MainActivity is active")
            }
            return
        }

        if (action == ACTION_EXPOSURE_STATE_UPDATED) {
            val token = intent.getStringExtra(EXTRA_TOKEN)
            if (token.isNullOrEmpty()) {
                log("Token not found")
                return
            }
            if (shouldStartHeadlessJsTask(context)) {
                startHeadlessJsTaskWorker(context, token)
            } else {
                startStateUpdateWorker(context, token)
            }
        }
    }

    private fun startStateUpdateWorker(context: Context, token: String) {
        log("startStateUpdateWorker", mapOf("token" to token))

        val workManager: WorkManager = WorkManager.getInstance(context)
        val inputData = Data.Builder()
            .putString(EXTRA_TOKEN, token)
            .build()

        workManager.enqueue(
            OneTimeWorkRequest.Builder(StateUpdatedWorker::class.java)
                .setInputData(inputData)
                .build()
        )
    }

    private fun startHeadlessJsTaskWorker(context: Context, token: String) {
        log("startHeadlessJsTaskWorker", mapOf("token" to token))

        val workManager: WorkManager = WorkManager.getInstance(context)
        workManager.enqueue(OneTimeWorkRequest.Builder(HeadlessJsTaskWorker::class.java).build())
    }

    interface Helper {
        fun onReceive(token: String)
    }
}
