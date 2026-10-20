# T53 Android release kit v1.0.2

- Added `.github/workflows/build-vivo-test-apks.yml`: hosted Android API 36/Gradle build for both debug APKs, signature/package-ID verification, SHA-256 hashes, and a downloadable test artifact.
- Added `.github/workflows/build-signed-release.yml`: manually triggered signed release build using four GitHub Actions secrets; validates release package IDs/signatures and uploads signed APK artifacts. It does not deploy the website.
- Added optional CI signing configuration to both Gradle projects. No signing password or keystore is stored in source files.
- Added `VIVO_TEST_GUIDE.md` with the Vivo install flow, private-repository setup, signing-key setup, production distribution path, and critical device-owner safety warning.
- Added `tools/verify-release-kit.py` offline sanity checks.
- Corrected the child dashboard URL constant to a complete HTTPS URL.
- Confirmed locally: 14 source/project sanity checks, 11 XML files, the publisher script's Node syntax, and the website page's inline JavaScript syntax pass.
- Not completed: neither hosted workflow has run; no APK binary has been compiled, signed, installed, or device-tested in this session.
