package com.thigazhini_labs.samuraijack.stages

data class StageConfig(
    val fogColor: FloatArray,
    val fogDensity: Float,
    val dialogs: List<String>
)

object Stages {
    val stagesList = listOf(
        StageConfig(
            fogColor = floatArrayOf(1.0f, 1.0f, 1.0f),
            fogDensity = 0.0f,
            dialogs = emptyList()
        )
    )
}
