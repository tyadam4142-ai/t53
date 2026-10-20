# T53 on your Vivo — fastest safe test path

## Get actual APK files

This ZIP contains the app source and a cloud-build workflow; it does **not** contain APK binaries. The included workflow compiles both apps on a GitHub-hosted Android build runner.

1. Create a **private** repository on your GitHub account. Do not make your business source public just to build it.
2. Extract this ZIP on your computer. Upload the **contents of this ZIP's root** (`android/`, `website/`, `docs/`, `.github/`, and the Markdown files) into the repository root, then commit to the `main` branch.
3. Open the repository's **Actions** tab, select **Build T53 Vivo test APKs**, and press **Run workflow**. The workflow also runs automatically when Android source changes are pushed to `main`.
4. When the run succeeds, open that run's **Artifacts** section. Download **`T53-Parent-TEST.apk`** or **`T53-Child-TEST.apk`** directly (each is uploaded as an individual file), or download **`T53-Vivo-Test-APKs-and-Report`** for both APKs plus checksums/package metadata.
5. Transfer an APK to your Vivo or download it from GitHub while browsing on the Vivo. Install the parent test APK first if you're just checking launch and dashboard access. Since the repository is private, you'll need to be signed into GitHub to access the artifacts.

If GitHub reports a build failure, open the failed workflow run and read the first Gradle error. Do not call the test build “published” unless the workflow succeeds and the installed app has been checked on a device.

## Install on Vivo

1. On the Vivo, download the APK and open it from Downloads/File Manager.
2. When Android asks, permit your browser or file manager to **Install unknown apps** for this one installation. This is the normal Android warning for APKs downloaded outside Play Store.
3. Install and launch **T53 Parent**. Sign in only with a test account you control. Confirm that the dashboard URL loads, login works, links behave, and Back navigation is sensible.
4. You can launch the **T53 Child** test build to inspect its initial setup screen. Installing it alone does **not** activate app blocking.

## Critical safety note for the Child app

The Child app is a fully managed/device-owner controller, not a regular parental-control app. Device-owner mode can prevent installing or uninstalling apps, suspend apps, block factory reset, and make T53 the home screen. The Android setup flow usually requires a clean/factory-reset device. **Do not provision your everyday Vivo with personal data.** Use a spare test phone you can wipe. Back up that spare phone first. Do not scan a provisioning QR or run `dpm set-device-owner` against your personal device just to see whether the APK opens.

For a full managed-device test, use a spare/factory-reset Android phone and follow `docs/ANDROID_CHILD_SETUP.md`. A debug test package has `.debug` on the end of its package ID and is not the same package as the production Child APK, so use the exact debug component/package from the guide when testing that variant.

## What these builds prove—and what they do not

- A successful Actions build proves that Gradle compiled and packaged both debug APK variants and that the workflow verified their APK signatures and package IDs.
- It does **not** prove that the parent dashboard/API is online, server endpoints match the app, policies behave correctly on Funtouch OS, QR provisioning succeeds, or kiosk/lockdown/removal flows are safe on your Vivo.
- These APKs are for testing only. They are debug-signed and the production publisher is expected to reject them.

## Production website release

After test approval, generate signed **release** APKs from both Android projects using your own release keystore. Never share or commit that keystore/passwords. Use `docs/ANDROID_PLAY_AND_DIRECT.md` and `website/backend/scripts/publish-android-apks.mjs` to validate the release signatures and publish the website downloads. Then download the two files from the live website and re-test them from a clean install before announcing availability.

## Optional: produce signed release APKs in GitHub Actions

Only do this after the debug build and real-device tests pass.

1. On a trusted computer with JDK installed, create a dedicated release keystore. Keep the file and passwords in at least two secure backups; losing the key makes future signed updates difficult or impossible.

   ```bash
   keytool -genkeypair -v -keystore t53-release.jks -alias t53-release -keyalg RSA -keysize 3072 -validity 10000
   ```

2. Base64-encode the keystore locally. On macOS/Linux:

   ```bash
   base64 < t53-release.jks | tr -d '\n'
   ```

   On Windows PowerShell:

   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("t53-release.jks"))
   ```

3. In the **private repository**, open **Settings → Secrets and variables → Actions** and create these repository secrets. Paste the base64 output into the first secret and the corresponding values into the remaining secrets:
   - `T53_RELEASE_KEYSTORE_BASE64`
   - `T53_RELEASE_STORE_PASSWORD`
   - `T53_RELEASE_KEY_ALIAS` (for the command above, `t53-release`)
   - `T53_RELEASE_KEY_PASSWORD`

   Never paste the keystore contents or passwords into chat, source files, commits, issues, or logs.

4. Open **Actions → Build signed T53 release APKs → Run workflow**. After it succeeds, download the individual `T53-Android.apk` and `T53-Parent-Android.apk` artifacts, plus `T53-Signed-Release-Verification`. Check certificate fingerprints, then test them on devices before publishing.

This workflow builds and uploads signed APK artifacts; it does **not** deploy your website. Publish them with the site's publisher/deployment flow only after checking the live backend, HTTPS download URLs and real-device behavior.
