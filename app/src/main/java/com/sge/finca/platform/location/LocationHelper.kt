package com.sge.finca.platform.location

import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationHelper @Inject constructor(
    private val client: FusedLocationProviderClient,
) {
    private val _location = MutableStateFlow(LocationState())
    val location: StateFlow<LocationState> = _location.asStateFlow()

    @SuppressLint("MissingPermission")
    suspend fun requestCurrentLocation(): LocationState? {
        val loc = try {
            client.lastLocation.await()
        } catch (_: Exception) {
            null
        }

        if (loc != null && isRecent(loc.time) && loc.accuracy <= MAX_ACCEPTABLE_ACCURACY) {
            val state = loc.toLocationState()
            _location.update { state }
            return state
        }

        return try {
            val fresh = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null,
            ).await()
            fresh?.toLocationState()?.also { state -> _location.update { state } }
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun trackLocation(intervalMs: Long = TRACKING_INTERVAL_MS): Flow<LocationState> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(MIN_DISPLACEMENT_METERS)
            .setWaitForAccurateLocation(true)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (loc.accuracy > MAX_ACCEPTABLE_ACCURACY) return
                val state = loc.toLocationState()
                _location.update { state }
                trySend(state)
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun android.location.Location.toLocationState() = LocationState(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        timestamp = time,
        isAvailable = true,
    )

    private fun isRecent(timestamp: Long): Boolean =
        System.currentTimeMillis() - timestamp < MAX_AGE_MS

    companion object {
        private const val TRACKING_INTERVAL_MS = 3_000L
        private const val MIN_DISPLACEMENT_METERS = 3f
        private const val MAX_ACCEPTABLE_ACCURACY = 50f
        private const val MAX_AGE_MS = 60_000L
    }
}
