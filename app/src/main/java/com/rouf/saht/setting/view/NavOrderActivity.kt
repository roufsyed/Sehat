package com.rouf.saht.setting.view

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.rouf.saht.R
import com.rouf.saht.common.activity.BaseActivity
import com.rouf.saht.databinding.ActivityNavOrderBinding
import io.paperdb.Paper

class NavOrderActivity : BaseActivity() {

    private lateinit var binding: ActivityNavOrderBinding
    private var orderChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val surfaceColor = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorSurface, 0
        )
        binding.appBar.setBackgroundColor(surfaceColor)
        binding.toolbar.setBackgroundColor(surfaceColor)
        window.statusBarColor = surfaceColor
        val isNight = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = !isNight

        val savedOrder = Paper.book().read<List<String>>(PREF_NAV_ORDER) ?: DEFAULT_ORDER
        val items = savedOrder.mapNotNull { key -> ALL_NAV_ITEMS.find { it.key == key } }.toMutableList()
        // Add any missing items at the end (in case new tabs were added)
        ALL_NAV_ITEMS.forEach { nav -> if (items.none { it.key == nav.key }) items.add(nav) }

        val adapter = NavOrderAdapter(items) {
            orderChanged = true
            Paper.book().write(PREF_NAV_ORDER, items.map { it.key })
        }

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                adapter.onItemMoved(from.bindingAdapterPosition, to.bindingAdapterPosition)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = false
        })

        adapter.touchHelper = touchHelper
        binding.rvNavOrder.layoutManager = LinearLayoutManager(this)
        binding.rvNavOrder.adapter = adapter
        touchHelper.attachToRecyclerView(binding.rvNavOrder)
    }

    override fun finish() {
        if (orderChanged) setResult(Activity.RESULT_OK)
        super.finish()
    }

    companion object {
        const val PREF_NAV_ORDER = "pref_nav_order"

        val DEFAULT_ORDER = listOf("dashboard", "pedometer", "heartRate", "meditation", "settings")

        val ALL_NAV_ITEMS = listOf(
            NavItem("dashboard",  "Home",        R.drawable.ic_home_black_24dp),
            NavItem("pedometer",  "Pedometer",   R.drawable.ic_walking),
            NavItem("heartRate",  "Heart Rate",  R.drawable.ic_heart),
            NavItem("meditation", "Meditation",  R.drawable.ic_meditation_man),
            NavItem("settings",   "Settings",    R.drawable.ic_settings_24)
        )

        fun getNavId(key: String): Int = when (key) {
            "dashboard"  -> R.id.navigation_dashboard
            "pedometer"  -> R.id.navigation_pedometer
            "heartRate"  -> R.id.navigation_heartRate
            "meditation" -> R.id.navigation_meditation
            "settings"   -> R.id.navigation_settings
            else         -> R.id.navigation_dashboard
        }
    }
}
