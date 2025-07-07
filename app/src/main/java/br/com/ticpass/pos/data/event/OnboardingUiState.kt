package br.com.ticpass.pos.data.event

import br.com.ticpass.pos.data.room.entity.EventEntity


/**
 * A sealed hierarchy describing the onboarding state for the for you screen.
 */
sealed interface OnboardingUiState {
    /**
     * The onboarding state is loading.
     */
    object Loading : OnboardingUiState

    /**
     * The onboarding state was unable to load.
     */
    object LoadFailed : OnboardingUiState

    /**
     * There is no onboarding state.
     */
    object NotShown : OnboardingUiState

    /**
     * There is a onboarding state, with the given lists of topics.
     */
    data class Shown(
        val events: List<EventEntity>,
    ) : OnboardingUiState {
        /**
         * True if the onboarding can be dismissed.
         */
        val isDismissable: Boolean get() = events.any { it.isSelected }
    }
}
