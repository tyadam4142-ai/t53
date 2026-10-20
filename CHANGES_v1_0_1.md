# T53 Android release kit: v1.0.1 review changes

## Found by compiling the sources (Kotlin 2.1.20 against android.jar API 36)
- **Parent app did not compile**: inside the WebView `apply {}` block, `progress` resolved to WebView's own deprecated `getProgress()` instead of the ProgressBar. Fixed (field renamed `progressBar`). Child app compiled cleanly.

## Fixed by code review
- Both apps: `kotlin { jvmToolchain(17) }` fails on Android Studio's bundled JDK 21 unless a JDK 17 is installed. Replaced with an explicit `jvmTarget = 17`, which works on JDK 17 or 21.
- Both apps: targetSdk 36 forces edge-to-edge, so content drew under the status/navigation bars and keyboard. Added window-inset padding (WebView in Parent; every screen in Child).
- Both apps: targetSdk 36 enables predictive back, which stops `onBackPressed()` being called (Parent could not go back in the dashboard; Child could not leave the request screen with Back). Opted out with `enableOnBackInvokedCallback="false"`.
- Parent: dark status-bar icons on a navy bar made them invisible. Set light icons.
- Both apps: no launcher icon. Added an adaptive "T" icon (vector, no binaries).
- Child: server errors using an `error` field (not `message`) showed only "Request failed (400)". Now shows the server's text.
- Publisher script: now rejects debug-certificate APKs, debuggable APKs, and APKs without a v2/v3 signature; prefers `aapt2`; checks the `qrcode` package before copying any file.

## Still NOT verified (needs Android Studio + real phones)
Gradle build, resource linking, signing, install, device-owner provisioning by QR, kiosk behaviour, parent/child sync against the live Worker.
