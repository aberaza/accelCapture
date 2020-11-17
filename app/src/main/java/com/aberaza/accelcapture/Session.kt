package com.aberaza.accelcapture

import kotlinx.serialization.*


@Serializable
data class Session(
    val uid: String,
    val sid: String,
    val sensorResolution: Float,
    val sensorMaxRange: Float,
    val startTime: Long,
    val samples: Int,
    val triggerAcceleration: Float,
    val triggerMethod: String,
    val sessionData: SessionData
)

@Serializable
data class SessionData(
    val duration: Long,
    val activity: String,
    val rate: Int,
    val accelerationX: MutableList<Float>?,
    val accelerationY: MutableList<Float>?,
    val accelerationZ: MutableList<Float>?
)