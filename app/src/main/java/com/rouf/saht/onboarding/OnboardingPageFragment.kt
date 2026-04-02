package com.rouf.saht.onboarding

interface OnboardingPageFragment {
    fun onNextTapped(proceed: () -> Unit) { proceed() }
}
