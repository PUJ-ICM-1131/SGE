package com.sge.finca.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["farmId", "type", "normalizedName"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val farmId: String,
    val name: String,
    val normalizedName: String,
    val type: String,
    val isDefault: Boolean,
    val syncStatus: String = "PENDING"
)