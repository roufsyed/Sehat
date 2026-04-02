package com.rouf.saht.common.model

data class HeartRateMonitorSettings(
    var duration: Int = 60,
    val unit: String = "Sec",
    var sensitivityLevel: HeartRateMonitorSensitivity = HeartRateMonitorSensitivity.MEDIUM,
)
