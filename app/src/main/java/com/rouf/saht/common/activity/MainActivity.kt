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

    /** Tracks the last nav order we applied so we only rebuild when it changes. */
    private var lastAppliedNavOrder: List<String>? = null

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

        // Apply custom nav order before wiring up the navigation controller,
        // so the menu is in the correct order when setupWithNavController syncs state.
        applyNavOrder(navView)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        // Two-way sync: tab taps navigate, destination changes update the selected tab.
        navView.setupWithNavController(navController)

        // On fresh launch, select the user's default screen.
        // Posted so it runs after setupWithNavController's initial sync
        // (which selects the nav graph's startDestination).
        if (savedInstanceState == null) {
            navView.post { selectScreen(intent) }
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
        // Only rebuild the menu if the saved order actually changed
        // (e.g. user returned from NavOrderActivity after reordering).
        // This avoids clearing the menu and resetting the selected tab
        // on every resume (coming back from Settings, Home Settings, etc.).
        val currentOrder = Paper.book().read<List<String>>(NavOrderActivity.PREF_NAV_ORDER)
        if (currentOrder != lastAppliedNavOrder) {
            applyNavOrder(binding.navView)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        selectScreen(intent)
    }

    /**
     * Selects the appropriate bottom-nav tab. If the intent carries an explicit
     * target (e.g. from a notification), that takes priority; otherwise falls
     * back to the user's configured default screen.
     */
    private fun selectScreen(intent: Intent?) {
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
        binding.navView.selectedItemId = navId
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    /**
     * Reorders the BottomNavigationView menu items to match the user's
     * saved preference, preserving the currently selected tab.
     *
     * Skips the rebuild entirely when the saved order is null (user has
     * never customised → use the XML-declared order).
     */
    private fun applyNavOrder(navView: BottomNavigationView) {
        val savedOrder = Paper.book().read<List<String>>(NavOrderActivity.PREF_NAV_ORDER)
        lastAppliedNavOrder = savedOrder
        if (savedOrder == null) return

        // Remember which tab is currently active so we can restore it
        // after the menu is cleared and rebuilt.
        val previousSelectedId = navView.selectedItemId

        val menu = navView.menu
        data class MenuEntry(val id: Int, val title: CharSequence, val icon: android.graphics.drawable.Drawable?)
        val originals = (0 until menu.size()).map { i ->
            menu.getItem(i).let { MenuEntry(it.itemId, it.title ?: "", it.icon) }
        }

        menu.clear()

        // Re-add items in the user's preferred order
        for ((order, key) in savedOrder.withIndex()) {
            val navId = NavOrderActivity.getNavId(key)
            val orig = originals.find { it.id == navId } ?: continue
            menu.add(0, orig.id, order, orig.title).icon = orig.icon
        }
        // Safety net: append any items not present in the saved order
        // (e.g. a new tab was added in an app update).
        for (orig in originals) {
            if (menu.findItem(orig.id) == null) {
                menu.add(0, orig.id, menu.size(), orig.title).icon = orig.icon
            }
        }

        // Restore selection. The guard prevents setting an invalid id
        // (0 on first launch before any tab is selected).
        if (previousSelectedId != 0 && menu.findItem(previousSelectedId) != null) {
            navView.selectedItemId = previousSelectedId
        }
    }

    companion object {
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }
}
