package com.rouf.saht.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.rouf.saht.common.activity.BaseActivity
import com.rouf.saht.R
import com.rouf.saht.common.activity.MainActivity
import com.rouf.saht.databinding.ActivityOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import io.paperdb.Paper

@AndroidEntryPoint
class OnboardingActivity : BaseActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var dots: Array<View>

    companion object {
        const val PREF_ONBOARDING_COMPLETE = "onboarding_complete"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        setupDots(adapter.itemCount)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateButtons(position)
            }
        })

        updateButtons(0)

        binding.btnBack.setOnClickListener {
            val cur = binding.viewPager.currentItem
            if (cur > 0) binding.viewPager.currentItem = cur - 1
        }

        binding.btnNext.setOnClickListener {
            val cur = binding.viewPager.currentItem
            if (cur < adapter.itemCount - 1) {
                val fragment = supportFragmentManager.findFragmentByTag("f$cur") as? OnboardingPageFragment
                fragment?.onNextTapped { binding.viewPager.currentItem = cur + 1 }
                    ?: run { binding.viewPager.currentItem = cur + 1 }
            } else {
                completeOnboarding()
            }
        }
    }

    private fun setupDots(count: Int) {
        binding.dotsContainer.removeAllViews()
        dots = Array(count) {
            View(this).apply {
                val size = resources.getDimensionPixelSize(R.dimen.dot_size)
                layoutParams = LinearLayout.LayoutParams(size, size).also { it.setMargins(8, 0, 8, 0) }
                background = ContextCompat.getDrawable(this@OnboardingActivity, R.drawable.bg_onboarding_dot)
            }.also { binding.dotsContainer.addView(it) }
        }
        updateDots(0)
    }

    private fun updateDots(position: Int) {
        val primaryColor = BaseActivity.effectivePrimary(this)
        val inactiveColor = getColor(R.color.dark_grey)
        dots.forEachIndexed { i, dot ->
            dot.backgroundTintList = ColorStateList.valueOf(
                if (i == position) primaryColor else inactiveColor
            )
        }
    }

    private fun updateButtons(position: Int) {
        val isFirst = position == 0
        val isLast = position == 3
        binding.btnBack.visibility = if (isFirst) View.INVISIBLE else View.VISIBLE
        binding.btnNext.text = if (isLast) "Finish" else "Next"
    }

    fun completeOnboarding() {
        Paper.book().write(PREF_ONBOARDING_COMPLETE, true)
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
