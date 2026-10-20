#!/usr/bin/env python3
"""Offline source/package sanity checks for the T53 Android release kit."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android"
errors = []
checks = []
def check(ok, success, failure):
    if ok:
        checks.append("PASS: " + success)
    else:
        errors.append("FAIL: " + failure)

for project in ("t53-parent", "t53-managed-child"):
    base = ANDROID / project
    check((base / "settings.gradle.kts").is_file(), f"{project}: Gradle settings found", f"{project}: missing settings.gradle.kts")
    check((base / "app/build.gradle.kts").is_file(), f"{project}: app build file found", f"{project}: missing app/build.gradle.kts")
    manifest = base / "app/src/main/AndroidManifest.xml"
    try:
        tree = ET.parse(manifest)
        app_id = tree.getroot().attrib.get("package")
        checks.append(f"PASS: {project}: manifest XML is well-formed")
    except Exception as exc:
        errors.append(f"FAIL: {project}: manifest parse: {exc}")
    gradle = (base / "app/build.gradle.kts").read_text(encoding="utf-8") if (base / "app/build.gradle.kts").exists() else ""
    check("compileSdk = 36" in gradle, f"{project}: compileSdk 36 configured", f"{project}: compileSdk 36 missing")

parent = (ANDROID / "t53-parent/app/build.gradle.kts").read_text(encoding="utf-8")
child = (ANDROID / "t53-managed-child/app/build.gradle.kts").read_text(encoding="utf-8")
check('applicationId = "com.t53.telecom53.parent"' in parent, "Parent release package ID matches website publisher", "Parent applicationId mismatch")
check('applicationId = "com.t53.telecom53.child"' in child, "Child release package ID matches website publisher", "Child applicationId mismatch")
check('applicationIdSuffix = ".debug"' in child, "Child test build is isolated with .debug package suffix", "Child debug suffix missing; test could conflict with production package")
workflow = ROOT / ".github/workflows/build-vivo-test-apks.yml"
check(workflow.is_file(), "GitHub Actions test build workflow exists", "Missing GitHub Actions test build workflow")
check(not list(ROOT.rglob("*.apk")), "No fake/prebuilt APKs are included in source kit", "APK files unexpectedly included; verify provenance and signature")
for f in (ROOT / "website/backend/scripts/publish-android-apks.mjs",):
    check(f.is_file(), f"Publisher present: {f.relative_to(ROOT)}", f"Publisher missing: {f}")

print("T53 release-kit offline checks")
for line in checks:
    print(line)
for line in errors:
    print(line)
print(f"\n{len(checks)} passed, {len(errors)} failed")
sys.exit(1 if errors else 0)
