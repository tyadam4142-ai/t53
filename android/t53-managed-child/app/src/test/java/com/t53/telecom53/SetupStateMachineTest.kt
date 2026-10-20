package com.t53.telecom53

import org.junit.Assert.assertEquals
import org.junit.Test

class SetupStateMachineTest {
    @Test
    fun freshInstallWithoutDeviceOwnerStaysOnPairingFlow() {
        assertEquals(
            ChildSetupState.NOT_PAIRED,
            SetupStateMachine.resolve(isPaired = false, isDeviceOwner = false, isReleased = false)
        )
    }

    @Test
    fun unpairedDeviceOwnerCanStillPairNormally() {
        assertEquals(
            ChildSetupState.NOT_PAIRED,
            SetupStateMachine.resolve(isPaired = false, isDeviceOwner = true, isReleased = false)
        )
    }

    @Test
    fun pairedOrdinaryInstallUsesStandardMode() {
        assertEquals(
            ChildSetupState.STANDARD_READY,
            SetupStateMachine.resolve(isPaired = true, isDeviceOwner = false, isReleased = false)
        )
    }

    @Test
    fun advancedModeRequiresPairingAndVerifiedDeviceOwner() {
        assertEquals(
            ChildSetupState.ADVANCED_READY,
            SetupStateMachine.resolve(isPaired = true, isDeviceOwner = true, isReleased = false)
        )
    }

    @Test
    fun releasedStateWinsOverPairingAndOwnership() {
        assertEquals(
            ChildSetupState.RELEASED,
            SetupStateMachine.resolve(isPaired = true, isDeviceOwner = true, isReleased = true)
        )
    }
}
