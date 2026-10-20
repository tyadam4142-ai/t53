# T53 Android Managed Child

The managed-device app applies parent-approved app rules only after Android successfully provisions T53 as the device owner. Without that setup it shows its setup instructions and does not pretend to be enforcing the policy.

## Build

- Recommended: use the repository-root GitHub Actions workflows in `../../.github/workflows/`.
- For local build, use Android Studio with JDK 17, Android SDK Platform 36 and Android build-tools 36.0.0. Gradle plugin is 8.9.1 and Kotlin plugin is 2.1.20; Gradle 8.11.1 is configured in `gradle/wrapper/gradle-wrapper.properties`.
- The source archive intentionally omits the Gradle wrapper launch scripts and JAR. Do not assume `./gradlew` exists. Use the cloud workflow or install Gradle 8.11.1 and invoke Gradle directly.
- For a signed CI release, see `VIVO_TEST_GUIDE.md` and configure the owner-controlled release keystore as GitHub Actions secrets. Never commit the key/passwords.

## App details
- Production package ID: `com.t53.telecom53.child` (debug builds append `.debug`).
- `minSdk` 28 (Android 9); `targetSdk` 36.
- Uses Android device-owner and lock-task APIs. Full restrictions can lock down a device and should be tested only on a spare/factory-reset phone.
- Precise location is not included in this build. Review `docs/ANDROID_PLAY_AND_DIRECT.md` before broader distribution.
