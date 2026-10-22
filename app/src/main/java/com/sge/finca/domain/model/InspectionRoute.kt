package com.sge.finca.domain.model

enum class RouteStatus { ACTIVE, PAUSED, COMPLETED }

data class InspectionRoute(
    val id: String = "",
    val farmId: String = "",
    val userId: String = "",
    val startTime: Long = 0L,
    val endTime: Long? = null,
    val status: RouteStatus = RouteStatus.ACTIVE,
    val totalDistanceMeters: Double = 0.0,
    val pointCount: Int = 0,
)
