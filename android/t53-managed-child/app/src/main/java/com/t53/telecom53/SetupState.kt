package com.t53.telecom53

enum class ChildSetupState {
    NOT_PAIRED,
    STANDARD_READY,
    ADVANCED_READY,
    RELEASED
}

object SetupStateMachine {
    fun resolve(isPaired: Boolean, isDeviceOwner: Boolean, isReleased: Boolean): ChildSetupState =
        when {
            isReleased -> ChildSetupState.RELEASED
            !isPaired -> ChildSetupState.NOT_PAIRED
            isDeviceOwner -> ChildSetupState.ADVANCED_READY
            else -> ChildSetupState.STANDARD_READY
        }
}
