package com.t53.telecom53

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserManager

/**
 * Applies the parent's rules to the phone. Everything here needs T53 to be the Android
 * "device owner" (see the setup screen). Without that Android does not allow any of it.
 */
object Enforcer {
    private val RESTRICTIONS = listOf(
        UserManager.DISALLOW_INSTALL_APPS,           // no new apps
        UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
        UserManager.DISALLOW_UNINSTALL_APPS,         // nothing can be deleted
        UserManager.DISALLOW_APPS_CONTROL,           // no force-stop / clear data / disable from Settings
        UserManager.DISALLOW_FACTORY_RESET,          // no wiping the phone to get rid of T53
        UserManager.DISALLOW_SAFE_BOOT,              // safe mode would pause device-owner rules
        UserManager.DISALLOW_ADD_USER
    )

    private fun dpm(ctx: Context) = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private fun admin(ctx: Context) = ComponentName(ctx, T53DeviceAdminReceiver::class.java)

    fun isOwner(ctx: Context): Boolean = dpm(ctx).isDeviceOwnerApp(ctx.packageName)

    /** The exact command a parent runs once on a fresh phone. */
    fun setupCommand(ctx: Context): String =
        "adb shell dpm set-device-owner ${ctx.packageName}/${T53DeviceAdminReceiver::class.java.name}"

    /** Apps with a launcher icon (package -> label), excluding T53 itself. */
    @Suppress("DEPRECATION")
    fun launchableApps(ctx: Context): Map<String, String> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val out = LinkedHashMap<String, String>()
        for (ri in pm.queryIntentActivities(intent, 0)) {
            val pkg = ri.activityInfo.packageName
            if (pkg == ctx.packageName || out.containsKey(pkg)) continue
            out[pkg] = ri.loadLabel(pm).toString()
        }
        return out.toList().sortedBy { it.second.lowercase() }.toMap(LinkedHashMap())
    }

    /** Cheap call so T53 may enter kiosk mode right after boot, before the first sync. */
    fun ensureLockTaskPackages(ctx: Context, approved: Set<String>) {
        if (!isOwner(ctx)) return
        runCatching { dpm(ctx).setLockTaskPackages(admin(ctx), (approved + ctx.packageName).toTypedArray()) }
    }

    /**
     * Locks the phone down to T53 plus [approved] apps. Safe to call repeatedly.
     * Returns true when the device-owner rules were applied.
     */
    fun applyPolicy(ctx: Context, approved: Set<String>): Boolean {
        if (!isOwner(ctx)) return false
        val d = dpm(ctx)
        val a = admin(ctx)
        val own = ctx.packageName
        val allowed = approved + own

        // 1) Kiosk: only T53 and approved apps may run on screen.
        runCatching { d.setLockTaskPackages(a, allowed.toTypedArray()) }
        runCatching {
            d.setLockTaskFeatures(a, DevicePolicyManager.LOCK_TASK_FEATURE_HOME or DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS)
        }

        // 2) Nothing can be installed, removed, force-stopped or reset.
        for (r in RESTRICTIONS) runCatching { d.addUserRestriction(a, r) }
        runCatching { d.setUninstallBlocked(a, own, true) }

        // 3) T53 is the home screen, and Android keeps it that way.
        runCatching {
            val home = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            d.clearPackagePersistentPreferredActivities(a, own)
            d.addPersistentPreferredActivity(a, home, ComponentName(ctx, MainActivity::class.java))
        }

        // 4) Defence in depth: every launchable app that is not approved is suspended.
        val installed = launchableApps(ctx).keys
        val block = installed.filter { it !in allowed }
        val open = installed.filter { it in allowed }
        if (open.isNotEmpty()) runCatching { d.setPackagesSuspended(a, open.toTypedArray(), false) }
        if (block.isNotEmpty()) runCatching { d.setPackagesSuspended(a, block.toTypedArray(), true) }
        return true
    }

    /** Called only after the parent approved removal with their password. Undoes everything. */
    fun release(ctx: Context): Boolean {
        if (!isOwner(ctx)) return true
        val d = dpm(ctx)
        val a = admin(ctx)
        val own = ctx.packageName
        runCatching { d.setLockTaskPackages(a, emptyArray()) }
        for (r in RESTRICTIONS) runCatching { d.clearUserRestriction(a, r) }
        val all = launchableApps(ctx).keys
        if (all.isNotEmpty()) runCatching { d.setPackagesSuspended(a, all.toTypedArray(), false) }
        runCatching { d.setUninstallBlocked(a, own, false) }
        runCatching { d.clearPackagePersistentPreferredActivities(a, own) }
        @Suppress("DEPRECATION")
        runCatching { d.clearDeviceOwnerApp(own) }
        return !isOwner(ctx)
    }
}
