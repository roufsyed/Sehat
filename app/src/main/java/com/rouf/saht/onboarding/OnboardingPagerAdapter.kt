package com.rouf.saht.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 4
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> OnboardingWelcomeFragment()
        1 -> OnboardingPermissionsFragment()
        2 -> OnboardingProfileFragment()
        else -> OnboardingGoalFragment()
    }
}
