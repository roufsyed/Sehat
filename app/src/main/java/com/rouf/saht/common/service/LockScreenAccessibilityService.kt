package com.rouf.saht.common.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class LockScreenAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
    }

    companion object {
        var instance: LockScreenAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val flat = ComponentName(context, LockScreenAccessibilityService::class.java)
                .flattenToString()
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(":").any { it.equals(flat, ignoreCase = true) }
        }

        /** Performs a soft lock that allows biometric (fingerprint/face) unlock. */
        fun lock(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                return instance != null
            }
            return false
        }
    }
}
