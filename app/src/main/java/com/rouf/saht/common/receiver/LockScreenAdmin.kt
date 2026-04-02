package com.rouf.saht.common.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class LockScreenAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {}
    override fun onDisabled(context: Context, intent: Intent) {}
}
