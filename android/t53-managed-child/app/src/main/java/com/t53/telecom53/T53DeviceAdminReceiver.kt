package com.t53.telecom53

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class T53DeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "T53 protects this phone. A parent can remove it from the T53 parent dashboard."

    /** QR / adb setup finished: open T53 so the license code can be entered right away. */
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        runCatching {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
