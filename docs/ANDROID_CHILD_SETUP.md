# T53 Child setup

T53 Child can be installed and opened as a normal Android app. Device Owner enrollment is an optional advanced mode, not a prerequisite for standard startup or pairing.

## Standard setup

1. Install T53 Child on the child's phone and open it.
2. Enter the T53 license code and the child's name, then select **Connect**.
3. Keep the phone online while the app checks in with the T53 service.
4. The Child screen reports whether the latest parent connection succeeded. If it cannot connect, retry after checking the network; the phone remains usable in standard mode.

The app declares network access only; it does not request Usage Access, Accessibility, location, or VPN permissions. In standard mode it does not block apps, make itself the home screen, or apply system restrictions. Use the phone normally.

### Backend connection status

The backend implementation is not present in this repository, so the pairing contract cannot be verified here. In particular, `Api.kt` currently sends the literal `******` as the `Authorization` header for authenticated requests. The enrollment response may be received while the follow-up authenticated status request fails. A locally saved token is not proof of a connected parent session; fix and verify the authorization scheme against the backend before relying on pairing.

## Optional advanced device management

Use this only for a dedicated device or a device that has been appropriately backed up and can be wiped. Android generally requires initial Device Owner provisioning on a clean or factory-reset device; exact steps vary by Android version and manufacturer.

1. In T53 Child, explicitly choose **Set up advanced device management** and review the warning.
2. Continue only if you intend to provision this test/child device. Cancel or use Back to return to ordinary setup without enrolling.
3. Follow the displayed command using the exact package ID and Device Admin receiver component shown for the installed build. A debug build has a different application ID suffix.
4. Run any provisioning command yourself from a trusted computer. T53 never invokes ADB or resets the device.
5. Return to T53 and check Device Owner status. The app uses Android's `DevicePolicyManager` to determine ownership; it does not infer it from the setup button or pairing response.

Do not factory-reset your everyday phone to try T53. Do not proceed if you do not understand or intend to complete the management setup.

### Features and limits

The current Child source applies app allow-list, kiosk, app-suspension, uninstall, and device restrictions only after Android confirms Device Owner status and only through the implemented `DevicePolicyManager` calls. Without that status, those restrictions are unavailable and the Child dashboard says so. Usage monitoring, Accessibility-based controls, and VPN filtering are not implemented.

Device Owner can significantly restrict a phone. Test it only on a dedicated, backed-up device. OEM behavior, including Funtouch OS background behavior, has not been physically verified in this repository.

## Build and regression tests

Use JDK 17, Android SDK Platform 36, Android build-tools 36.0.0, and Gradle 8.11.1. The repository has no Gradle wrapper launch scripts or wrapper JAR; run the GitHub Actions test workflow or install Gradle 8.11.1 and invoke it directly.

```sh
python3 tools/verify-child-startup.py
gradle --no-daemon -p android/t53-managed-child testDebugUnitTest
gradle --no-daemon -p android/t53-managed-child assembleDebug
```

The unit test covers fresh install without Device Owner, standard mode after pairing, and the requirement that advanced mode needs both pairing and verified Device Owner status. The Python check validates that the Activity routes those states to the matching screens and keeps Device Owner instructions behind the explicit advanced path.
