# Merge the Android publisher into the existing T53 backend

**This folder is a patch, not a full copy of the backend. Do not replace your whole `backend/` folder or its `package.json` with this subset.** Keep the existing T53 backend, its Worker source, D1 migrations, `wrangler.toml`, `deploy.sh`, `package-lock.json`, and dependencies.

1. Copy this kit's `public/download/index.html` to the existing backend's `public/download/index.html`.
2. Copy `scripts/publish-android-apks.mjs` to the existing backend's `scripts/publish-android-apks.mjs`.
3. In the **existing** `backend/package.json`, add this entry inside its current `scripts` object, keeping all other scripts:

   ```json
   "publish-android-apks": "node scripts/publish-android-apks.mjs"
   ```

   The T53 source archive already declares `qrcode` under `devDependencies`; if working in a different checkout, ensure `qrcode` is installed for the publisher script. Run `npm install` in `backend/` if dependencies need to be restored.

4. From `backend/`, run the command below, substituting your real signed release APK paths and version:

   ```bash
   npm run publish-android-apks -- /path/to/child-release.apk /path/to/parent-release.apk 1.0.0
   ```

   The Child APK must use package ID `com.t53.telecom53.child`; the Parent APK must use `com.t53.telecom53.parent`. The script checks APK signatures, rejects the Android debug certificate/debuggable APKs, enforces the 25 MiB per-file limit, and writes files/metadata/QR into `backend/public/download/`.

5. Deploy using the original backend's deployment process (the source archive includes `./deploy.sh`). Then verify both APK URLs, their SHA-256 values, metadata, and the Child QR using a separate browser/device.

The publisher checks binary metadata; it cannot prove that the backend APIs, login, parent policies, or Vivo device-owner behavior work. Complete real-device tests before a public release.
