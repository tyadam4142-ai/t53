package com.t53.telecom53

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Boot: re-apply the rules at once. Alarm: a temporary approval just ended. */
class T53Receiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, PolicyEngine.ACTION_EXPIRE -> {
                val pending = goAsync()
                Thread {
                    try {
                        PolicyEngine.applyLocal(context)
                        if (intent.action == Intent.ACTION_BOOT_COMPLETED) SyncJobService.schedule(context)
                    } finally { pending.finish() }
                }.start()
            }
        }
    }
}
