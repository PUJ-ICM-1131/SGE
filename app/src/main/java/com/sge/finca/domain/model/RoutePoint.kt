package com.sge.finca.domain.model

data class RoutePoint(
    val id: String = "",
    val routeId: String = "",
    val order: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val timestamp: Long = 0L,
)
