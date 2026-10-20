# T53 Android: locked child phone, approved apps, parent password

## What the parent gets
1. **Dashboard password.** First sign-in uses an emailed code, then the parent creates a password (8+ characters, a letter and a number). After that: email + password. 5 wrong tries lock the account for 15 minutes. "Forgot password" = emailed code, then choose a new one.
2. **Approved apps.** In the dashboard, open a child and tick the apps they may use. Everything else on the phone is blocked. The phone applies changes within about a minute when online, and re-checks at least every 15 minutes in the background.
3. **Cannot be deleted by the child.** T53 is the phone's device owner: it cannot be uninstalled, force-stopped, cleared, or removed by factory reset, safe mode, or adding a new user.
4. **Removal only with the parent's password.** Dashboard > child > "Remove T53 from this phone" > enter password > "Allow removal". The phone unlocks itself the next time it is online, tells the server, and then T53 can be uninstalled. "Keep protected" cancels it before the phone picks it up.
5. **Ask for an app.** The child can ask for a blocked app. The parent gets "Allow 30 min" or "Deny". When allowed, the app opens for that time and then blocks again by itself.

## One-time phone setup (required)
Android only allows these protections for a device owner, and only on a phone with **no accounts added yet** (new, or factory reset and skip adding a Google account).

### Easiest: QR code (no computer)
1. Publish the APK (see `ANDROID_PLAY_AND_DIRECT.md`). That creates the QR on https://t53.dynamicrobotics53.com/download/.
2. Factory reset the child phone (or use a new one). On the first Welcome screen tap the same spot **6 times**.
3. Connect to Wi-Fi when asked, then scan the QR code from the download page (open it on another screen).
4. Android downloads and installs T53 and makes it the device owner. Wait until it finishes.
5. Open T53, enter the license code and the child's name.
6. In the dashboard, open the child and tick the apps to allow (tick **Phone** so the child can call).

### Alternative: adb (computer)
1. Install the T53 child APK (do not sign in to any account).
2. Settings > About phone > tap Build number 7 times. Then Developer options > USB debugging on.
3. Run exactly:
   ```
   adb shell dpm set-device-owner com.t53.telecom53.child/com.t53.telecom53.T53DeviceAdminReceiver
   ```
   The T53 setup screen shows (and can copy) this command for the installed build.
4. Continue from step 5 above.

Until the apps are ticked, nothing but T53 can be opened, by design.

## Honest limits
- Without device-owner setup the phone cannot be locked. T53 shows the setup screen instead of pretending it is protected.
- A phone that is offline keeps its last rules. New rules apply when it reconnects.
- If the phone is switched off and held by someone with physical and technical access to the bootloader, no app can stop that. Normal use by a child cannot reach it.
- After "Allow removal" completes, the phone is retired from that license. Ask the admin for a new license to protect it again.
- Emergency calls: Android's emergency dialer works from the lock screen regardless of this app list.

## Deploy the backend
```bash
cd backend
./deploy.sh        # applies migrations (including 0004) and deploys the worker
```
Migration `0004_parent_password_app_policy.sql` adds the password columns, the app list and the approved-apps table.

## Test the backend locally (no cloud account needed)
```bash
cd backend
npm install
npx wrangler --config ./wrangler.toml d1 migrations apply t53-telecom-53-db --local
npx wrangler --config ./wrangler.toml dev --local --port 8788 \
  --var AUTH_SECRET:local-test-secret-local-test-secret-123 --var T53_ENV:development
# in a second terminal:
node scripts/e2e-local.mjs
```
Expected last line: `43 passed, 0 failed`.

## Troubleshooting
- `dpm set-device-owner` says "already several users/accounts": remove all accounts (or factory reset) and retry.
- Says "not allowed to set the device owner because there are already some accounts": same cause.
- Apps still open: the phone has not synced yet. Open T53 and wait a minute, or check it is online.
- Dashboard says wrong password after 5 tries: wait 15 minutes, or use "forgot password".

## Email codes (required for first sign-in and password reset)
The server sends the 6-digit code through Resend. Set these once, then redeploy:
```bash
cd backend
npx wrangler --config ./wrangler.toml secret put RESEND_API_KEY
npx wrangler --config ./wrangler.toml secret put FROM_EMAIL     # e.g. T53 <no-reply@dynamicrobotics53.com>, a sender verified in Resend
npx wrangler --config ./wrangler.toml secret put AUTH_SECRET    # a long random string
npx wrangler --config ./wrangler.toml secret list
```
Without these, the dashboard now says "email delivery is not set up" instead of pretending a code was sent.

## Check the live server rejects wrong codes
```bash
curl -s https://t53.dynamicrobotics53.com/health
curl -s -X POST https://t53.dynamicrobotics53.com/v1/auth/verify-otp -H 'content-type: application/json' -d '{"email":"you@example.com","code":"123456"}'
```
The second command must answer `"error":"otp_invalid"`. If it returns a `token`, the server is running old code: redeploy with `./deploy.sh`.
