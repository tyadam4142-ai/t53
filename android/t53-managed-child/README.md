# T53 Child for Android

T53 Child has two modes:

- **Standard mode:** install and open the app, then enter the T53 license code and the child's name to pair. The app remains usable without Device Owner enrollment. Standard mode reports a connection status but does not block apps or apply Android system restrictions.
- **Advanced device management:** an optional path opened only when the user taps **Set up advanced device management**. Device Owner status is checked through Android's `DevicePolicyManager`; a pairing response or saved preference is not treated as enrollment.

App blocking, kiosk mode, app suspension, uninstall protection, and device restrictions are only attempted after Android confirms T53 is the Device Owner. Usage Access, Accessibility monitoring, and VPN filtering are not implemented by this app. See [`docs/ANDROID_CHILD_SETUP.md`](../../docs/ANDROID_CHILD_SETUP.md) before enabling advanced management.

The current repository does not include the backend implementation. Backend pairing/authentication and physical-device behavior have not been verified; do not treat local enrollment state as proof that the parent connection works.

## Build and tests

- Package ID: `com.t53.telecom53.child` (debug builds append `.debug`).
- `minSdk` 28 (Android 9); `targetSdk` 36.
- Build tools: JDK 17, Android SDK Platform 36, Android build-tools 36.0.0, Gradle 8.11.1.
- Run `testDebugUnitTest` for the Child setup-state regression tests, then `assembleDebug` to build the test APK.
- The repository intentionally omits Gradle wrapper launch scripts and the wrapper JAR. Use the GitHub Actions workflow or install the documented Gradle version locally.
