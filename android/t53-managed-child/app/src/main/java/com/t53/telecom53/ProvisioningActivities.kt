package com.t53.telecom53

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Android 12+ asks the device-management app which setup mode it wants during QR provisioning.
 * T53 always wants a fully managed device (this is what enables uninstall and app blocking).
 */
class GetProvisioningModeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val result = Intent().putExtra(EXTRA_PROVISIONING_MODE, PROVISIONING_MODE_FULLY_MANAGED_DEVICE)
        setResult(RESULT_OK, result)
        finish()
    }

    private companion object {
        const val EXTRA_PROVISIONING_MODE = "android.app.extra.PROVISIONING_MODE"
        const val PROVISIONING_MODE_FULLY_MANAGED_DEVICE = 1
    }
}

/** Android finishes setup by asking the management app to confirm it is ready. T53 is ready at once. */
class PolicyComplianceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_OK)
        finish()
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
