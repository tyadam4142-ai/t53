#!/usr/bin/env python3
"""Regression checks for ordinary T53 Child startup and enrollment gating."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / "android/t53-managed-child/app/src/main/java/com/t53/telecom53/MainActivity.kt").read_text()
POLICY = (ROOT / "android/t53-managed-child/app/src/main/java/com/t53/telecom53/PolicyEngine.kt").read_text()
TEST = (ROOT / "android/t53-managed-child/app/src/test/java/com/t53/telecom53/SetupStateMachineTest.kt").read_text()

checks = {
    "startup routing uses the setup state machine": "SetupStateMachine.resolve(store.token != null, owner, store.released)" in MAIN,
    "fresh installs route to pairing, not mandatory enrollment": "ChildSetupState.NOT_PAIRED -> if (showingAdvancedSetup) showSetup() else showEnroll()" in MAIN,
    "paired non-owners route to ordinary mode": "ChildSetupState.STANDARD_READY -> if (showingAdvancedSetup) showSetup() else showStandardHub()" in MAIN,
    "advanced instructions require explicit state": "if (showingAdvancedSetup) showSetup() else showEnroll()" in MAIN,
    "advanced setup warns before instructions": "generally requires a factory-reset device" in MAIN and "Do not continue unless you understand" in MAIN,
    "advanced setup has a cancel/back path": 'setNegativeButton("Cancel", null)' in MAIN and '"Back to standard setup"' in MAIN,
    "standard mode reports unenforced status before managed policy sync": (
        "if (!Enforcer.isOwner(app))" in POLICY
        and "Api.status(st.deviceId, token, battery(app), false" in POLICY
        and "SyncResult.STANDARD_READY" in POLICY
        and "run(app, st)" not in POLICY.split("if (!Enforcer.isOwner(app)) {", 1)[1].split("return try {", 1)[0]
        and "Enforcer.applyPolicy" not in POLICY.split("if (!Enforcer.isOwner(app)) {", 1)[1].split("return try {", 1)[0]
    ),
    "state regression tests cover fresh install and ordinary mode": (
        "freshInstallWithoutDeviceOwnerStaysOnPairingFlow" in TEST
        and "pairedOrdinaryInstallUsesStandardMode" in TEST
    ),
    "no startup route directly forces the legacy setup screen": "!owner && store.token == null -> showSetup()" not in MAIN
        and "!owner -> showSetup()" not in MAIN,
}

for name, passed in checks.items():
    print(f"{'PASS' if passed else 'FAIL'}: {name}")

print(f"\n{sum(checks.values())} passed, {len(checks) - sum(checks.values())} failed")
sys.exit(0 if all(checks.values()) else 1)
