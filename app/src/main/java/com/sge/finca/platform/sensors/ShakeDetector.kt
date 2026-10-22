package com.sge.finca.platform.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val threshold: Float = 12f,
    private val cooldownMillis: Long = 2_000L,
) : SensorEventListener {

    init {
        require(threshold > 0f)
        require(cooldownMillis >= 2_000L)
    }

    private val sensorManager = context.applicationContext
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isAvailable: Boolean
        get() = accelerometer != null

    private val gravity = FloatArray(3)
    private var gravityInitialized = false

    private var isListening = false
    private var onShake: (() -> Unit)? = null

    private var aboveThreshold = false
    private var peakCount = 0
    private var firstPeakMillis = 0L
    private var lastPeakMillis = 0L

    private var lastShakeMillis: Long? = null

    fun start(onShake: () -> Unit): Boolean {
        val sensor = accelerometer ?: return false

        this.onShake = onShake

        if (isListening) return true

        resetReadings()

        isListening = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_GAME,
        )

        if (!isListening) {
            this.onShake = null
        }

        return isListening
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        isListening = false
        onShake = null
        resetReadings()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isListening) return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        if (!gravityInitialized) {
            for (axis in 0..2) {
                gravity[axis] = event.values[axis]
            }

            gravityInitialized = true
            return
        }

        var magnitudeSquared = 0f

        for (axis in 0..2) {
            gravity[axis] =
                GRAVITY_ALPHA * gravity[axis] +
                        (1f - GRAVITY_ALPHA) * event.values[axis]

            val linearAcceleration = event.values[axis] - gravity[axis]
            magnitudeSquared += linearAcceleration * linearAcceleration
        }

        val magnitude = sqrt(magnitudeSquared)
        val now = SystemClock.elapsedRealtime()

        val previousShake = lastShakeMillis
        if (previousShake != null && now - previousShake < cooldownMillis) {
            peakCount = 0
            aboveThreshold = false
            return
        }

        if (magnitude < threshold * RESET_RATIO) {
            aboveThreshold = false
        }

        if (magnitude < threshold || aboveThreshold) return

        aboveThreshold = true

        if (peakCount > 0 && now - lastPeakMillis < MIN_PEAK_GAP_MILLIS) {
            return
        }

        if (peakCount == 0 || now - firstPeakMillis > SHAKE_WINDOW_MILLIS) {
            firstPeakMillis = now
            peakCount = 1
        } else {
            peakCount++
        }

        lastPeakMillis = now

        if (peakCount >= REQUIRED_PEAKS) {
            lastShakeMillis = now
            peakCount = 0
            onShake?.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun resetReadings() {
        gravity.fill(0f)
        gravityInitialized = false
        aboveThreshold = false
        peakCount = 0
        firstPeakMillis = 0L
        lastPeakMillis = 0L
    }

    private companion object {
        const val GRAVITY_ALPHA = 0.8f
        const val RESET_RATIO = 0.5f
        const val REQUIRED_PEAKS = 3
        const val MIN_PEAK_GAP_MILLIS = 80L
        const val SHAKE_WINDOW_MILLIS = 800L
    }
}