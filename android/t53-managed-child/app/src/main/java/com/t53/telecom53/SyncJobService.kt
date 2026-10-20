package com.t53.telecom53

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context

/** Keeps rules fresh while the child is inside an approved app and T53's home screen is not visible. */
class SyncJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Thread {
            try { PolicyEngine.sync(applicationContext) } finally { jobFinished(params, false) }
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    companion object {
        fun schedule(ctx: Context) {
            val js = ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            if (js.getPendingJob(Config.JOB_ID) != null) return
            val job = JobInfo.Builder(Config.JOB_ID, ComponentName(ctx, SyncJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(Config.JOB_INTERVAL_MS)
                .setPersisted(true)
                .build()
            js.schedule(job)
        }

        fun cancel(ctx: Context) {
            (ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).cancel(Config.JOB_ID)
        }
    }
}
