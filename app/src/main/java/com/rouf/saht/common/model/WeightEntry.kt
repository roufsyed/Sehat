package com.rouf.saht.common.model

data class WeightEntry(
    val weightKg: Double,
    val timestamp: Long = System.currentTimeMillis()
)
