package com.t53.telecom53

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build

enum class SyncResult { OK, STANDARD_READY, NOT_ENROLLED, NOT_OWNER, OFFLINE, RELEASED, ERROR }

/** Talks to the T53 server and makes the phone match what the parent chose. */
object PolicyEngine {
    const val ACTION_EXPIRE = "com.t53.telecom53.ACTION_EXPIRE"

    /** Network + enforcement. Blocking: call from a background thread. */
    @Synchronized
    fun sync(ctx: Context): SyncResult {
        val app = ctx.applicationContext
        val st = Store(app)
        if (st.released) {
            confirmReleaseIfNeeded(st)
            return SyncResult.RELEASED
        }
        val token = st.token ?: return SyncResult.NOT_ENROLLED
        if (!Enforcer.isOwner(app)) {
            st.protectionActive = false
            st.connectionHealthy = false
            return try {
                Api.status(st.deviceId, token, battery(app), false, appVersion(app), "Android ${Build.VERSION.RELEASE}")
                st.lastSyncMillis = System.currentTimeMillis()
                st.connectionHealthy = true
                SyncResult.STANDARD_READY
            } catch (_: ApiException) {
                SyncResult.ERROR
            } catch (_: Exception) {
                SyncResult.OFFLINE
            }
        }
        return try {
            try {
                run(app, st)
            } catch (e: ApiException) {
                if (e.status == 401 && renewSession(st)) run(app, st) else throw e
            }
        } catch (e: ApiException) {
            st.connectionHealthy = false
            applyLocal(app)
            if (e.status == 403 || e.status == 401) SyncResult.ERROR else SyncResult.OFFLINE
        } catch (_: Exception) {
            st.connectionHealthy = false
            applyLocal(app) // offline: the last rules stay in force
            SyncResult.OFFLINE
        }
    }

    private fun run(app: Context, st: Store): SyncResult {
        val id = st.deviceId
        val tok = st.token ?: return SyncResult.NOT_ENROLLED

        // Parent approved a one-off "let me use this app" request.
        val decisions = Api.decisions(id, tok).optJSONArray("decisions")
        if (decisions != null) {
            for (i in 0 until decisions.length()) {
                val d = decisions.getJSONObject(i)
                val pkg = d.optString("packageName", "")
                if (d.optString("type") == "appAccess" && pkg.isNotBlank()) {
                    val secs = d.optLong("durationSeconds", Config.DEFAULT_GRANT_SECONDS)
                    st.addGrant(pkg, System.currentTimeMillis() + secs * 1000L)
                    Api.ackDecision(id, tok, d.getString("id"))
                }
            }
        }

        // Parent's approved apps and the removal switch.
        val pol = Api.policy(id, tok)
        val list = pol.optJSONArray("allowedPackages")
        val approved = HashSet<String>()
        if (list != null) for (i in 0 until list.length()) approved.add(list.getString(i))
        st.allowed = approved
        st.policyVersion = pol.optInt("version", 0)

        if (pol.optBoolean("releaseRequested", false)) {
            // Password-protected removal approved in the parent dashboard.
            st.releaseUnconfirmed = true
            val done = Enforcer.release(app)
            if (done) {
                st.released = true
                st.protectionActive = false
                confirmReleaseIfNeeded(st)
                return SyncResult.RELEASED
            }
        }

        // Tell the dashboard which apps exist on this phone (only when the list changed).
        val installed = Enforcer.launchableApps(app)
        val h = installed.hashCode()
        if (h != st.appsHash) {
            Api.reportApps(id, tok, installed)
            st.appsHash = h
        }

        val enforced = Enforcer.applyPolicy(app, st.effectiveAllowed())
        st.protectionActive = enforced
        scheduleExpiry(app, st)

        Api.status(id, tok, battery(app), enforced, appVersion(app), "Android ${Build.VERSION.RELEASE}")
        st.lastSyncMillis = System.currentTimeMillis()
        st.connectionHealthy = true
        return SyncResult.OK
    }

    /** The old 30-day session ended: sign in again quietly with the stored license. */
    private fun renewSession(st: Store): Boolean {
        val code = st.licenseCode ?: return false
        return try {
            val r = Api.enroll(code, st.childName.ifBlank { "Child" }, st.deviceId)
            st.token = r.getString("deviceToken")
            true
        } catch (_: Exception) { false }
    }

    private fun confirmReleaseIfNeeded(st: Store) {
        if (!st.releaseUnconfirmed) return
        val tok = st.token ?: return
        try {
            Api.released(st.deviceId, tok)
            st.releaseUnconfirmed = false
        } catch (e: ApiException) {
            if (e.status == 401 || e.status == 403 || e.status == 409) st.releaseUnconfirmed = false
        } catch (_: Exception) { /* try again next time */ }
    }

    /** No network: re-apply what we last knew (also expires temporary approvals on time). */
    fun applyLocal(ctx: Context) {
        val app = ctx.applicationContext
        val st = Store(app)
        if (st.released || st.token == null || !Enforcer.isOwner(app)) return
        Enforcer.applyPolicy(app, st.effectiveAllowed())
        scheduleExpiry(app, st)
    }

    /** Wake up exactly when the next temporary approval ends so the app is blocked again. */
    private fun scheduleExpiry(ctx: Context, st: Store) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            ctx, 0, Intent(ctx, T53Receiver::class.java).setAction(ACTION_EXPIRE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val next = st.activeGrants().values.minOrNull()
        if (next == null) am.cancel(pi) else am.set(AlarmManager.RTC_WAKEUP, next + 1000L, pi)
    }

    private fun battery(ctx: Context): Int {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return 0
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return if (level < 0 || scale <= 0) 0 else (level * 100 / scale).coerceIn(0, 100)
    }

    private fun appVersion(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0.0"
    } catch (_: Exception) { "1.0.0" }
}
