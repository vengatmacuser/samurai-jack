package com.thigazhini_labs.samuraijack.engine

import kotlin.math.sqrt

data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    operator fun plus(v: Vector3) = Vector3(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vector3) = Vector3(x - v.x, y - v.y, z - v.z)
    operator fun times(s: Float) = Vector3(x * s, y * s, z * s)
    
    fun dot(v: Vector3): Float = x * v.x + y * v.y + z * v.z
    fun cross(v: Vector3) = Vector3(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x
    )
    
    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun normalize(): Vector3 {
        val len = length()
        return if (len > 0f) Vector3(x / len, y / len, z / len) else Vector3(0f, 0f, 0f)
    }

    fun dist(v: Vector3): Float {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}


