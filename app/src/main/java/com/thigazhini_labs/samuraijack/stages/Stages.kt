package com.thigazhini_labs.samuraijack.stages

data class StageConfig(
    val fogColor: FloatArray,
    val fogDensity: Float,
    val dialogs: List<String>,
    val bossType: String
)

object Stages {
    val stagesList = listOf(
        StageConfig(
            fogColor = floatArrayOf(0.1f, 0.14f, 0.18f),
            fogDensity = 0.02f,
            dialogs = emptyList(),
            bossType = "None"
        )
    )
}
