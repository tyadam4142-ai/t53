# T53 Android release kit (source + website publishing support)

**Status: v1.0.2 source + CI build preparation. No APKs have been built in this session.** The uploaded source archive contained no APK binaries. The current workspace has no Android SDK, Gradle executable/wrapper JAR, or signing tools, and it cannot resolve the Android/Gradle download hosts. The previous v1.0.1 notes claim a Kotlin-only type check, but that check could not be independently rerun here. This ZIP is source plus reproducible hosted build workflows—not an APK and not a claim of successful compilation or device testing.

## Contents
- `android/t53-managed-child/`: the T53 managed-child Android source from the saved T53_RELEASE archive.
- `android/t53-parent/`: a parent Android wrapper that opens the production parent dashboard over HTTPS. This replaces the saved source's placeholder screen; it is not a separate native rewrite of the web dashboard.
- `website/backend/public/download/index.html`: dual-download page for Parent and Child APKs, with a legacy metadata fallback.
- `website/backend/scripts/publish-android-apks.mjs`: validates signatures/package IDs and publishes both APKs plus the child provisioning QR.
- `website/backend/PUBLISHER_SETUP.md`: explains how to merge the patch into the full backend without overwriting existing Worker/package files.

## Fastest route to installable Vivo test APKs
1. Follow `VIVO_TEST_GUIDE.md`: put the **contents of this ZIP's root** in a private GitHub repository.
2. Open **Actions → Build T53 Vivo test APKs → Run workflow**. The workflow builds both debug APKs on a GitHub-hosted runner, verifies signatures/package IDs, and uploads individual `.apk` files for 14 days.
3. Under the successful run's **Artifacts** section, download `T53-Parent-TEST.apk` or `T53-Child-TEST.apk` directly. Install Parent for ordinary launch/dashboard testing. Child is for setup-screen checks unless you have a spare/factory-reset Android test device.

The workflow is included but has **not** been run from this session. It gives you a reproducible cloud build path; this ZIP itself still contains no APK binaries.

## Production build and website publishing
1. After test builds pass, create a dedicated signing keystore and store its values as GitHub Actions secrets, as described in `VIVO_TEST_GUIDE.md`.
2. Run **Actions → Build signed T53 release APKs**. It signs the two release variants, checks package IDs and release flags, and uploads `T53-Android.apk` and `T53-Parent-Android.apk` as separate artifacts. Alternatively, generate signed APKs through Android Studio using the same long-term keystore.
3. Keep the keystore and passwords private and back them up securely. Never commit them, and do not publish debug APKs. Do not lose the key: updates must use the same signing identity.
4. Use `website/backend/scripts/publish-android-apks.mjs` and `docs/ANDROID_PLAY_AND_DIRECT.md` to validate and publish the two signed files, metadata, and Child setup QR.
5. Deploy using your existing website release procedure. Verify both download URLs, SHA-256 values, and setup QR from an external browser/device before advertising release availability. The workflow does not deploy your site.

## Important limitations
- The Child app requires Android device-owner provisioning for app blocking and uninstall restrictions. Simply installing it will not enable those protections. Device-owner setup can lock down a phone and normally requires a clean/factory-reset device; do not use your everyday Vivo phone for provisioning.
- The Parent APK is a secure HTTPS WebView shell, not a separate native rewrite of the web dashboard. Its functionality depends on `https://t53.dynamicrobotics53.com/parent/` being live and working.
- The workflow's test APKs are debug-signed and are not for public production distribution. The CI workflow and real-device behavior have not been run/verified from this session.
