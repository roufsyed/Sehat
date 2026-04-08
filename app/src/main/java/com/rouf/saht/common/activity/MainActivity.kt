package com.rouf.saht.common.activity

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rouf.saht.R
import com.rouf.saht.common.service.LockScreenAccessibilityService
import com.rouf.saht.databinding.ActivityMainBinding
import com.rouf.saht.onboarding.OnboardingActivity
import com.rouf.saht.setting.view.CustomizationActivity
import com.rouf.saht.setting.view.NavOrderActivity
import com.rouf.saht.setting.view.SettingsFragment
import dagger.hilt.android.AndroidEntryPoint
import io.paperdb.Paper

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Paper.book().read(OnboardingActivity.PREF_ONBOARDING_COMPLETE, false) != true) {
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        val navView: BottomNavigationView = binding.navView
        applyNavOrder(navView)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        navView.setupWithNavController(navController)

        if (savedInstanceState == null) {
            navigateToRequestedScreen(intent)
        }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val isEnabled = Paper.book().read(SettingsFragment.PREF_DOUBLE_TAP_LOCK, false) ?: false
                if (isEnabled) {
                    LockScreenAccessibilityService.lock()
                }
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        applyNavOrder(binding.navView)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigateToRequestedScreen(intent)
    }

    private fun navigateToRequestedScreen(intent: Intent?) {
        val target = intent?.getStringExtra(EXTRA_NAVIGATE_TO)
        val navId = if (target != null) {
            CustomizationActivity.defaultScreenNavId(target)
        } else {
            val defaultScreen = Paper.book().read(
                CustomizationActivity.PREF_DEFAULT_SCREEN,
                CustomizationActivity.SCREEN_DASHBOARD
            ) ?: CustomizationActivity.SCREEN_DASHBOARD
            CustomizationActivity.defaultScreenNavId(defaultScreen)
        }
        if (navId != R.id.navigation_dashboard) {
            binding.navView.selectedItemId = navId
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun applyNavOrder(navView: BottomNavigationView) {
        val savedOrder = Paper.book().read<List<String>>(NavOrderActivity.PREF_NAV_ORDER)
            ?: return // default XML order

        val menu = navView.menu
        // Store original menu items
        data class MenuEntry(val id: Int, val title: CharSequence, val icon: android.graphics.drawable.Drawable?)
        val originals = mutableListOf<MenuEntry>()
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            originals.add(MenuEntry(item.itemId, item.title ?: "", item.icon))
        }

        menu.clear()

        // Re-add in saved order
        for ((order, key) in savedOrder.withIndex()) {
            val navId = NavOrderActivity.getNavId(key)
            val orig = originals.find { it.id == navId } ?: continue
            menu.add(0, orig.id, order, orig.title).icon = orig.icon
        }
        // Add any items not in saved order (safety)
        for (orig in originals) {
            if (menu.findItem(orig.id) == null) {
                menu.add(0, orig.id, menu.size(), orig.title).icon = orig.icon
            }
        }
    }

    companion object {
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }
}
