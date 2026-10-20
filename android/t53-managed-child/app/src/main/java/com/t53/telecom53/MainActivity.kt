package com.t53.telecom53

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * T53 Child supports ordinary pairing. System restrictions are available only in
 * the optional, Android-verified Device Owner mode.
 */
class MainActivity : Activity() {
    private val bg = Color.rgb(7, 16, 29)
    private val gold = Color.rgb(235, 195, 81)
    private val card = Color.rgb(18, 30, 49)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var store: Store
    private var busy = false
    private var showingRequestScreen = false
    private var showingAdvancedSetup = false
    private var lastScreen = ""

    private val poll = object : Runnable {
        override fun run() {
            syncNow(silent = true)
            handler.postDelayed(this, Config.UI_POLL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        SyncJobService.schedule(this)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
        handler.removeCallbacks(poll)
        handler.post(poll)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(poll)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            showingRequestScreen -> { showingRequestScreen = false; render() }
            showingAdvancedSetup -> { showingAdvancedSetup = false; render() }
            else -> super.onBackPressed()
        }
    }

    // ---------------------------------------------------------------- state

    private fun render() {
        val owner = Enforcer.isOwner(this)
        when (SetupStateMachine.resolve(store.token != null, owner, store.released)) {
            ChildSetupState.RELEASED -> showReleased()
            ChildSetupState.NOT_PAIRED -> if (showingAdvancedSetup) showSetup() else showEnroll()
            ChildSetupState.STANDARD_READY -> if (showingAdvancedSetup) showSetup() else showStandardHub()
            ChildSetupState.ADVANCED_READY -> {
                if (showingAdvancedSetup) showSetup()
                else if (showingRequestScreen) showRequestScreen()
                else { showHub(); enterKioskIfNeeded() }
            }
        }
    }

    private fun syncNow(silent: Boolean) {
        if (busy) return
        busy = true
        Thread {
            val result = try { PolicyEngine.sync(applicationContext) } catch (_: Exception) { SyncResult.ERROR }
            runOnUiThread {
                busy = false
                if (isFinishing) return@runOnUiThread
                if (!silent) toast(when (result) {
                    SyncResult.OK -> "Updated"
                    SyncResult.STANDARD_READY -> "Parent connection updated"
                    SyncResult.OFFLINE -> "No connection. The last rules stay on."
                    SyncResult.NOT_ENROLLED -> "Pair this phone with your parent first."
                    SyncResult.NOT_OWNER -> "Advanced device management is unavailable."
                    else -> "Could not update"
                })
                // Re-draw only if something that changes the screen changed.
                val sig = signature()
                if (sig != lastScreen) render()
            }
        }.start()
    }

    private fun signature(): String =
        "${store.released}|${store.token != null}|${Enforcer.isOwner(this)}|${store.effectiveAllowed().sorted()}|${store.lastSyncMillis}|${store.protectionActive}|$showingRequestScreen|$showingAdvancedSetup"

    private fun enterKioskIfNeeded() {
        if (!Enforcer.isOwner(this) || store.released || store.token == null) return
        Enforcer.ensureLockTaskPackages(this, store.effectiveAllowed())
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE && dpm.isLockTaskPermitted(packageName)) {
            try { startLockTask() } catch (_: Exception) { /* retried on next resume */ }
        }
    }

    // ---------------------------------------------------------------- screens

    private fun showSetup() {
        lastScreen = signature()
        val cmd = Enforcer.setupCommand(this)
        val root = column()
        root.addView(title("T53"))
        root.addView(body("Optional advanced device management", 20f, Color.WHITE, bold = true))
        root.addView(body(
            "This mode is for a dedicated or appropriately backed-up test or child device. Initial Device Owner provisioning generally requires a factory-reset device. Exact steps depend on the phone and Android version. Do not continue unless you understand and intend to complete setup.",
            15f, Color.LTGRAY))
        root.addView(body(
            "If you choose to proceed, enable Developer options and USB debugging on that device, connect it to a computer, then run this command. T53 will not run ADB or reset the phone.",
            15f, Color.LTGRAY))
        root.addView(TextView(this).apply {
            text = cmd; textSize = 13f; typeface = Typeface.MONOSPACE; setTextColor(gold)
            setBackgroundColor(card); setPadding(dp(14), dp(12), dp(14), dp(12)); setTextIsSelectable(true)
        }, matchWrap(top = dp(8)))
        root.addView(button("Copy command") {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("T53 setup", cmd)); toast("Copied")
        })
        root.addView(button("Check Device Owner status") {
            showingAdvancedSetup = false
            render()
            if (!Enforcer.isOwner(this)) toast("Device Owner is not enabled. Standard mode remains available.")
        })
        root.addView(button("Back to standard setup") { showingAdvancedSetup = false; render() })
        setContentView(scroll(root))
    }

    private fun showEnroll() {
        lastScreen = signature()
        val root = column()
        root.addView(title("T53"))
        root.addView(body("Welcome to T53 Child", 20f, Color.WHITE, bold = true))
        root.addView(body("Pair with your parent to connect this phone. Standard mode does not block apps or apply system restrictions.", 15f, Color.LTGRAY))
        root.addView(body("Connect this phone to your parent's T53 account.", 16f, Color.LTGRAY))
        val license = field("T53 license code", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        val name = field("Child's name", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val msg = body("", 14f, Color.rgb(255, 138, 128))
        val go = button("Connect") { }
        go.setOnClickListener {
            val code = license.text.toString().trim().uppercase()
            val child = name.text.toString().trim()
            if (code.isEmpty() || child.isEmpty()) { msg.text = "Enter the license code and the child's name."; return@setOnClickListener }
            go.isEnabled = false; msg.setTextColor(Color.LTGRAY); msg.text = "Connecting…"
            Thread {
                var error: String? = null
                try {
                    val r = Api.enroll(code, child, store.deviceId)
                    store.token = r.getString("deviceToken")
                    store.childName = child
                    PolicyEngine.sync(applicationContext)
                } catch (e: ApiException) {
                    error = e.message
                } catch (_: Exception) {
                    error = "No internet connection. Try again."
                }
                runOnUiThread {
                    if (error != null) { go.isEnabled = true; msg.setTextColor(Color.rgb(255, 138, 128)); msg.text = error }
                    else render()
                }
            }.start()
        }
        root.addView(license, matchWrap(top = dp(16)))
        root.addView(name, matchWrap(top = dp(8)))
        root.addView(go, matchWrap(top = dp(12)))
        root.addView(msg, matchWrap(top = dp(8)))
        root.addView(button("Set up advanced device management") {
            AlertDialog.Builder(this)
                .setTitle("Advanced setup")
                .setMessage("Device Owner setup is optional and generally requires a dedicated or backed-up device that may need a factory reset. Your everyday phone will not be reset by T53. Continue only if you intend to complete this setup.")
                .setPositiveButton("Continue") { _, _ -> showingAdvancedSetup = true; render() }
                .setNegativeButton("Cancel", null)
                .show()
        }, matchWrap(top = dp(20)))
        setContentView(scroll(root))
    }

    private fun showStandardHub() {
        lastScreen = signature()
        val root = column()
        root.addView(title("T53"))
        root.addView(body(
            if (store.childName.isNotBlank()) "Hi ${store.childName}" else "T53 Child",
            20f, Color.WHITE, bold = true))
        root.addView(body("This phone is paired with your parent in standard mode.", 15f, Color.LTGRAY))

        val recentSync = store.lastSyncMillis > 0L &&
            store.connectionHealthy &&
            System.currentTimeMillis() - store.lastSyncMillis <= 45L * 60L * 1000L
        val connection = when {
            recentSync -> "Connected to parent"
            store.lastSyncMillis > 0L -> "Connection unavailable. Last successful sync: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(store.lastSyncMillis))}"
            else -> "Not connected yet. Check your internet connection and retry."
        }
        root.addView(body(connection, 14f, if (recentSync) Color.rgb(82, 211, 170) else Color.rgb(255, 190, 90)), matchWrap(top = dp(12)))
        root.addView(body(
            "App blocking, kiosk mode, uninstall protection, and device restrictions are unavailable in standard mode. You can continue using the phone normally.",
            15f, Color.LTGRAY), matchWrap(top = dp(12)))
        root.addView(button("Retry connection") { syncNow(silent = false) })
        root.addView(button("Set up advanced device management") {
            AlertDialog.Builder(this)
                .setTitle("Advanced setup")
                .setMessage("Device Owner setup is optional and generally requires a dedicated or backed-up device that may need a factory reset. Your everyday phone will not be reset by T53. Continue only if you intend to complete this setup.")
                .setPositiveButton("Continue") { _, _ -> showingAdvancedSetup = true; render() }
                .setNegativeButton("Cancel", null)
                .show()
        })
        setContentView(scroll(root))
    }

    private fun showHub() {
        lastScreen = signature()
        val pm = packageManager
        val allowed = store.effectiveAllowed()
        val apps = Enforcer.launchableApps(this).filter { it.key in allowed }

        val root = column()
        root.addView(title("T53"))
        root.addView(body(
            if (store.childName.isNotBlank()) "Hi ${store.childName}" else "Your apps",
            18f, Color.WHITE, bold = true))

        if (apps.isEmpty()) {
            root.addView(body("No apps are approved yet. Ask your parent to choose apps in the T53 dashboard.", 15f, Color.LTGRAY), matchWrap(top = dp(16)))
        } else {
            val grid = GridLayout(this).apply { columnCount = 3 }
            for ((pkg, label) in apps) {
                val tile = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(6), dp(10), dp(6), dp(10))
                    isClickable = true
                    setOnClickListener { launch(pkg) }
                }
                val icon = ImageView(this).apply {
                    try { setImageDrawable(pm.getApplicationIcon(pkg)) } catch (_: Exception) { }
                }
                tile.addView(icon, LinearLayout.LayoutParams(dp(56), dp(56)))
                tile.addView(TextView(this).apply {
                    text = label; textSize = 12f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                    maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
                val lp = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                    width = 0
                }
                grid.addView(tile, lp)
            }
            root.addView(grid, matchWrap(top = dp(16)))
        }

        root.addView(button("Ask my parent for another app") { showingRequestScreen = true; render() }, matchWrap(top = dp(24)))
        val status = when {
            store.lastSyncMillis == 0L -> "Connecting to your parent…"
            store.protectionActive && System.currentTimeMillis() - store.lastSyncMillis <= 45L * 60L * 1000L -> "Managed restrictions applied"
            store.protectionActive -> "Last known managed restrictions applied; connection status is unknown"
            else -> "Managed restrictions are not confirmed"
        }
        root.addView(body(status, 12f, Color.GRAY), matchWrap(top = dp(12)))
        setContentView(scroll(root))
    }

    private fun showRequestScreen() {
        lastScreen = signature()
        val allowed = store.effectiveAllowed()
        val others = Enforcer.launchableApps(this).filter { it.key !in allowed }
        val root = column()
        root.addView(title("Ask a parent"))
        root.addView(body("Pick an app. Your parent gets a request and can allow it for a while.", 15f, Color.LTGRAY))
        if (others.isEmpty()) root.addView(body("There are no other apps on this phone.", 15f, Color.LTGRAY), matchWrap(top = dp(16)))
        for ((pkg, label) in others) {
            root.addView(button(label) {
                AlertDialog.Builder(this)
                    .setTitle(label)
                    .setMessage("Ask your parent to allow $label for ${Config.DEFAULT_GRANT_SECONDS / 60} minutes?")
                    .setPositiveButton("Ask") { _, _ -> sendRequest(pkg, label) }
                    .setNegativeButton("Cancel", null)
                    .show()
            })
        }
        root.addView(button("Back") { showingRequestScreen = false; render() }, matchWrap(top = dp(20)))
        setContentView(scroll(root))
    }

    private fun showReleased() {
        lastScreen = signature()
        runCatching { stopLockTask() }
        SyncJobService.cancel(this)
        val root = column()
        root.addView(title("T53"))
        root.addView(body("Your parent unlocked this phone.", 20f, Color.WHITE, bold = true))
        root.addView(body("All T53 restrictions are off. You can remove the T53 app now.", 15f, Color.LTGRAY))
        root.addView(button("Remove T53") {
            startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))
        }, matchWrap(top = dp(16)))
        root.addView(button("Open phone settings") { startActivity(Intent(Settings.ACTION_SETTINGS)) })
        setContentView(scroll(root))
    }

    // ---------------------------------------------------------------- actions

    private fun launch(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) { toast("This app cannot be opened"); return }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
            toast("This app cannot be opened")
        }
    }

    private fun sendRequest(pkg: String, label: String) {
        val tok = store.token ?: return
        Thread {
            val msg = try {
                Api.requestApp(store.deviceId, tok, pkg, "Please allow $label", Config.DEFAULT_GRANT_SECONDS)
                "Request sent to your parent"
            } catch (e: ApiException) { e.message ?: "Could not send" }
            catch (_: Exception) { "No internet connection" }
            runOnUiThread { toast(msg); showingRequestScreen = false; render() }
        }.start()
    }

    // ---------------------------------------------------------------- tiny UI helpers

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun column() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(24), dp(20), dp(28))
    }

    private fun scroll(inner: View) = ScrollView(this).apply {
        setBackgroundColor(bg); isFillViewport = true; addView(inner)
        // Android 15+ draws apps edge-to-edge: keep content clear of the status/navigation bars.
        setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
                v.setPadding(b.left, b.top, b.right, b.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
    }

    private fun matchWrap(top: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = top }

    private fun title(t: String) = TextView(this).apply {
        text = t; textSize = 30f; setTextColor(gold); typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 0, 0, dp(10))
    }

    private fun body(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun field(hint: String, inputType: Int) = EditText(this).apply {
        this.hint = hint; this.inputType = inputType
        setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
        setBackgroundColor(card); setPadding(dp(14), dp(14), dp(14), dp(14))
        isSingleLine = true
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text; isAllCaps = false; textSize = 15f
        setTextColor(Color.rgb(7, 16, 29)); setBackgroundColor(gold)
        minimumHeight = dp(52)
        setOnClickListener { onClick() }
        layoutParams = matchWrap(top = dp(10))
    }
}
