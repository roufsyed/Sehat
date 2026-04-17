package com.rouf.saht.dashboard

enum class DashboardCardId(val key: String) {
    STEPS("steps"),
    HR_BMI_ROW("hr_bmi_row"),
    WEEKLY_CHART("weekly_chart"),
    HR_ZONES("hr_zones"),
    DISTANCE("distance"),
    CALORIES("calories"),
    ACTIVE_DURATION("active_duration"),
    BPM_ACTIVITY("bpm_activity"),
    PEAK_BPM("peak_bpm"),
    RECOVERY("recovery"),
    CORRELATION("correlation"),
    INSIGHTS("insights"),
    WEEKLY_SUMMARY("weekly_summary"),
    RECORDS("records");

    companion object {
        const val PREF_CARD_ORDER = "dashboard_card_order"
        val DEFAULT_ORDER: List<String> = values().map { it.key }
    }
}
