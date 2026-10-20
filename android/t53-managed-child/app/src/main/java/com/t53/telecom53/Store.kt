package com.t53.telecom53

import android.content.Context
import org.json.JSONObject
import java.util.UUID

/** Small private key-value store. App-private storage: other apps cannot read it. */
class Store(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("t53_state", Context.MODE_PRIVATE)

    val deviceId: String
        @Synchronized get() {
            var v = sp.getString("deviceId", null)
            if (v == null) {
                v = "and-" + UUID.randomUUID().toString()
                sp.edit().putString("deviceId", v).apply()
            }
            return v
        }

    var token: String?
        get() = sp.getString("token", null)
        set(v) { sp.edit().putString("token", v).apply() }

    /** Kept so the phone can quietly renew its 30-day session. Never shown on screen. */
    var licenseCode: String?
        get() = sp.getString("licenseCode", null)
        set(v) { sp.edit().putString("licenseCode", v).apply() }

    var childName: String
        get() = sp.getString("childName", "") ?: ""
        set(v) { sp.edit().putString("childName", v).apply() }

    var allowed: Set<String>
        get() = HashSet(sp.getStringSet("allowed", emptySet()) ?: emptySet())
        set(v) { sp.edit().putStringSet("allowed", HashSet(v)).apply() }

    var policyVersion: Int
        get() = sp.getInt("policyVersion", 0)
        set(v) { sp.edit().putInt("policyVersion", v).apply() }

    var appsHash: Int
        get() = sp.getInt("appsHash", 0)
        set(v) { sp.edit().putInt("appsHash", v).apply() }

    /** True once the parent approved removal and this phone unlocked itself. */
    var released: Boolean
        get() = sp.getBoolean("released", false)
        set(v) { sp.edit().putBoolean("released", v).apply() }

    /** The phone is unlocked locally but the server has not been told yet. */
    var releaseUnconfirmed: Boolean
        get() = sp.getBoolean("releaseUnconfirmed", false)
        set(v) { sp.edit().putBoolean("releaseUnconfirmed", v).apply() }

    var protectionActive: Boolean
        get() = sp.getBoolean("protectionActive", false)
        set(v) { sp.edit().putBoolean("protectionActive", v).apply() }

    var lastSyncMillis: Long
        get() = sp.getLong("lastSync", 0L)
        set(v) { sp.edit().putLong("lastSync", v).apply() }

    /** Temporary parent approvals: package -> expiry (epoch millis). */
    private fun readGrants(): MutableMap<String, Long> {
        val out = HashMap<String, Long>()
        try {
            val o = JSONObject(sp.getString("grants", "{}") ?: "{}")
            for (k in o.keys()) out[k] = o.getLong(k)
        } catch (_: Exception) { }
        return out
    }

    @Synchronized
    fun addGrant(pkg: String, expiresAtMillis: Long) {
        val m = readGrants()
        m[pkg] = maxOf(expiresAtMillis, m[pkg] ?: 0L)
        sp.edit().putString("grants", JSONObject(m as Map<*, *>).toString()).apply()
    }

    @Synchronized
    fun activeGrants(now: Long = System.currentTimeMillis()): Map<String, Long> {
        val all = readGrants()
        val live = all.filterValues { it > now }
        if (live.size != all.size) sp.edit().putString("grants", JSONObject(live as Map<*, *>).toString()).apply()
        return live
    }

    /** Everything the child may open right now. */
    fun effectiveAllowed(): Set<String> = allowed + activeGrants().keys

    fun clearAll() { sp.edit().clear().apply() }
}
