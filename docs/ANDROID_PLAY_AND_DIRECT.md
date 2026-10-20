# T53 Android distribution: direct website download

T53 has two Android apps:

- **T53 Parent** (`com.t53.telecom53.parent`): a native Android shell that displays the hosted parent dashboard at `https://t53.dynamicrobotics53.com/parent/`. The dashboard/API must be live and reachable for the app to be useful.
- **T53 Managed Child** (`com.t53.telecom53.child`): a device-management app. App blocking, kiosk mode and restrictions only work after valid Android device-owner provisioning. Installing the APK alone does not activate those controls.

## Test before you publish

The root `.github/workflows/build-vivo-test-apks.yml` workflow compiles both debug variants in GitHub Actions and uploads each test APK as its own downloadable artifact. Use `VIVO_TEST_GUIDE.md` to run it and install the Parent test APK on a Vivo. The Child debug APK is suitable for checking that the setup screen opens; full device-owner tests should use only a spare/factory-reset phone that can be wiped.

For distribution on the website, do not publish debug APKs. Create a release keystore you control, save it securely, configure the GitHub Actions secrets described in `VIVO_TEST_GUIDE.md`, and run `.github/workflows/build-signed-release.yml`. Keep the same signing key for future updates. Losing that key can prevent existing installs from accepting the next update.

## Publishing both signed APKs

Merge the two website patch files in `website/backend/` into your **existing full backend**, following `website/backend/PUBLISHER_SETUP.md`. That guide specifically tells you not to overwrite the original backend `package.json` or Worker files.

From the original `backend/` folder, run:

```bash
npm run publish-android-apks -- /path/to/child-release.apk /path/to/parent-release.apk 1.0.0
./deploy.sh
```

Or set `T53_PUBLIC_ORIGIN` to the final HTTPS origin before running the publisher. It validates both APKs, checks their package IDs and release signing, rejects a debug certificate/debuggable binary, and writes the child provisioning QR and metadata. The `T53-Android.apk` URL remains the legacy-compatible Child download URL; the Parent APK is `T53-Parent-Android.apk`.

After deploying, verify these URLs from outside your development machine:

- `https://t53.dynamicrobotics53.com/download/`
- `https://t53.dynamicrobotics53.com/download/T53-Android.apk`
- `https://t53.dynamicrobotics53.com/download/T53-Parent-Android.apk`
- `https://t53.dynamicrobotics53.com/download/apks.json`
- `https://t53.dynamicrobotics53.com/download/provision-qr.svg`

Use the actual production domain if it differs from the default origin. Confirm every asset returns successfully and its displayed SHA-256 matches the generated metadata.

## Device-owner safety

The Child QR is for fully managed device provisioning, which is usually performed during initial setup on a clean/factory-reset Android phone. Provisioning can prevent app installation/uninstallation, suspend other apps, restrict reset/settings and make T53 the home screen. **Do not provision your everyday Vivo phone or any device holding personal data.** Use a spare test phone, back it up, and expect that it may need to be wiped to recover from a failed test.

## Scope and policy

This kit targets API 36. Direct APK distribution is distinct from publishing in Google Play; hosting an APK does not by itself make the product compliant with all applicable laws, platform policies, or privacy obligations. Review the product's data collection, parent consent, child privacy disclosures, and device-management behavior before distributing it beyond private testing.
