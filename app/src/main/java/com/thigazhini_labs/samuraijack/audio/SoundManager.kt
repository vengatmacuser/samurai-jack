package com.thigazhini_labs.samuraijack.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import com.thigazhini_labs.samuraijack.R
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin
import kotlin.random.Random

object SoundManager {
    private const val TAG = "SoundManager"
    private const val SAMPLE_RATE = 22050

    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var synthJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // Synthesizer active parameters
    @Volatile var isRunning = false
    @Volatile var currentStage = 1
    @Volatile var isLowHealthHeartbeatActive = false

    // Sound effect trigger states
    // Stores: soundName -> Time index or decay samples left
    private val activeEffects = ConcurrentHashMap<String, Int>()
    private val effectEnvelopes = ConcurrentHashMap<String, Float>()
    private val effectFrequencies = ConcurrentHashMap<String, Float>()

    fun start(context: Context) {
        if (isRunning) return
        isRunning = true

        try {
            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.soundtrack).apply {
                isLooping = true
                setVolume(0.2f, 0.2f)
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load/play official soundtrack MediaPlayer", e)
        }
        
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize.coerceAtLeast(4096),
                AudioTrack.MODE_STREAM
            )
            
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
            return
        }

        synthJob = coroutineScope.launch {
            val buffer = ShortArray(1024)
            var sampleIndex = 0L

            while (isRunning) {
                for (i in buffer.indices) {
                    val time = sampleIndex + i
                    var mixVal = 0.0

                    // 1. Procedural Background Music Generator (Disabled to play official soundtrack)
                    // mixVal += generateMusic(time, noteStep) * 0.25

                    // 2. Heartbeat synthesis (low health)
                    if (isLowHealthHeartbeatActive) {
                        mixVal += generateHeartbeat(time) * 0.4
                    }

                    // 3. Sound Effects Mixing
                    mixVal += mixSoundEffects()

                    // Clamp values to prevent clipping
                    val finalShort = (mixVal * Short.MAX_VALUE).coerceIn(
                        Short.MIN_VALUE.toDouble(),
                        Short.MAX_VALUE.toDouble()
                    ).toInt().toShort()
                    
                    buffer[i] = finalShort
                }

                audioTrack?.write(buffer, 0, buffer.size)
                sampleIndex += buffer.size
            }
        }
    }

    fun stop() {
        isRunning = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop AudioTrack", e)
        }
        audioTrack = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release MediaPlayer", e)
        }
        mediaPlayer = null
    }

    fun triggerSlash() {
        activeEffects["slash"] = 0
        effectEnvelopes["slash"] = 1.0f
    }

    fun triggerHit() {
        activeEffects["hit"] = 0
        effectEnvelopes["hit"] = 1.0f
    }

    fun triggerLaser() {
        activeEffects["laser"] = 0
        effectEnvelopes["laser"] = 1.0f
    }

    fun triggerExplosion() {
        activeEffects["explosion"] = 0
        effectEnvelopes["explosion"] = 1.0f
    }

    fun triggerSpecialCharge() {
        activeEffects["charge"] = 0
        effectEnvelopes["charge"] = 1.0f
    }

    // Procedural Music Engine matching each level's specific theme
    private fun generateMusic(time: Long, noteStep: Int): Double {
        val t = time.toDouble() / SAMPLE_RATE

        // Pentatonic Scale notes (frequencies in Hz)
        val kotoScale = doubleArrayOf(220.0, 247.0, 262.0, 330.0, 392.0, 440.0)
        val industrialScale = doubleArrayOf(110.0, 130.8, 146.8, 164.8, 196.0, 220.0)

        return when (currentStage) {
            1, 5, 13 -> { // Epic Battle (Aku / Ronin) - Heavy Taiko drum & metallic synths
                // Beat sequencer (heavy base frequency kick)
                val beat = noteStep % 8
                val isKick = (beat == 0 || beat == 3 || beat == 4 || beat == 6)
                val drum = if (isKick) {
                    val drumPhase = (time % (SAMPLE_RATE / 4)).toDouble() / SAMPLE_RATE
                    sin(2.0 * Math.PI * 65.0 * drumPhase) * Math.exp(-35.0 * drumPhase)
                } else 0.0

                // Tension synth note
                val noteIdx = (noteStep / 4) % kotoScale.size
                val freq = kotoScale[noteIdx]
                val synth = sin(2.0 * Math.PI * freq * t) * (0.5 + 0.5 * sin(2.0 * Math.PI * 4.0 * t))
                
                drum * 0.7 + synth * 0.3
            }
            2, 3, 4 -> { // Traditional Japanese (Prince / Training) - Zen bamboo flute & soft koto plucks
                val beat = noteStep % 16
                val playNote = (beat == 0 || beat == 4 || beat == 8 || beat == 10 || beat == 12)
                if (playNote) {
                    val noteIdx = (noteStep) % kotoScale.size
                    val freq = kotoScale[noteIdx]
                    val envelope = Math.exp(-2.5 * ((time % (SAMPLE_RATE / 2)).toDouble() / SAMPLE_RATE))
                    sin(2.0 * Math.PI * freq * t) * envelope
                } else {
                    0.0
                }
            }
            6, 7, 8 -> { // Cyberpunk Futurology (Machines / Portals) - Fast arpeggiated synth wave
                // 16th note arpeggiation (very fast)
                val arpeggio = noteStep % 4
                val noteIdx = (noteStep / 4 + arpeggio) % industrialScale.size
                val freq = industrialScale[noteIdx]
                val envelope = Math.exp(-8.0 * ((time % (SAMPLE_RATE / 8)).toDouble() / SAMPLE_RATE))
                
                // Sawtooth wave-like buzz
                val phase = (time.toDouble() * freq / SAMPLE_RATE) % 1.0
                val saw = (phase * 2.0 - 1.0) * envelope
                saw * 0.6
            }
            else -> { // Dark Wastelands (Beast / Lava Guardian) - Ominous wind drone & low tension pulses
                val lowDrone = sin(2.0 * Math.PI * 82.41 * t) // Low E drone
                val dynamicWobble = sin(2.0 * Math.PI * 0.5 * t) // slow swell
                lowDrone * (0.6 + 0.4 * dynamicWobble)
            }
        }
    }

    private fun generateHeartbeat(time: Long): Double {
        val t = time.toDouble() / SAMPLE_RATE
        val beatRate = 1.2 // beats per second
        val beatCycle = (time % (SAMPLE_RATE / beatRate)).toDouble() / SAMPLE_RATE
        
        // Double pulse simulation (lub-dub)
        val pulse1 = sin(2.0 * Math.PI * 65.0 * beatCycle) * Math.exp(-40.0 * beatCycle)
        val pulse2 = if (beatCycle > 0.15) {
            val offsetCycle = beatCycle - 0.15
            sin(2.0 * Math.PI * 55.0 * offsetCycle) * Math.exp(-40.0 * offsetCycle)
        } else 0.0

        return pulse1 + pulse2
    }

    private fun mixSoundEffects(): Double {
        var effectMix = 0.0

        // Slash effect: Rapid frequency slide down
        activeEffects["slash"]?.let { sampleOffset ->
            val env = effectEnvelopes["slash"] ?: 0f
            if (env > 0.01f) {
                val samplesPlayed = sampleOffset + 1024
                activeEffects["slash"] = samplesPlayed

                // Frequency sweep from 2500Hz down to 800Hz
                val duration = SAMPLE_RATE * 0.15
                val progress = sampleOffset.toDouble() / duration
                val currentFreq = 2500.0 - 1700.0 * progress.coerceIn(0.0, 1.0)
                
                val phase = (sampleOffset.toDouble() * currentFreq / SAMPLE_RATE)
                effectMix += sin(2.0 * Math.PI * phase) * env * 0.7
                
                // Decay envelope
                effectEnvelopes["slash"] = env * 0.95f
            } else {
                activeEffects.remove("slash")
                effectEnvelopes.remove("slash")
            }
        }

        // Metal Hit effect: Resonant ring + noise
        activeEffects["hit"]?.let { sampleOffset ->
            val env = effectEnvelopes["hit"] ?: 0f
            if (env > 0.01f) {
                activeEffects["hit"] = sampleOffset + 1024

                val t = sampleOffset.toDouble() / SAMPLE_RATE
                // Metallic ring component (high frequency + subharmonic)
                val ring = sin(2.0 * Math.PI * 1800.0 * t) * Math.exp(-25.0 * t) +
                           sin(2.0 * Math.PI * 1250.0 * t) * Math.exp(-15.0 * t)
                
                // Noise click component (sword contact crunch)
                val click = (Random.nextFloat() * 2.0f - 1.0f) * Math.exp(-100.0 * t)

                effectMix += (ring * 0.5 + click * 0.5) * env * 0.8
                effectEnvelopes["hit"] = env * 0.92f
            } else {
                activeEffects.remove("hit")
                effectEnvelopes.remove("hit")
            }
        }

        // Laser blast effect: Sweep down from 3200Hz to 400Hz
        activeEffects["laser"]?.let { sampleOffset ->
            val env = effectEnvelopes["laser"] ?: 0f
            if (env > 0.01f) {
                activeEffects["laser"] = sampleOffset + 1024

                val duration = SAMPLE_RATE * 0.3
                val progress = sampleOffset.toDouble() / duration
                val freq = 3200.0 - 2800.0 * progress.coerceIn(0.0, 1.0)

                val phase = (sampleOffset.toDouble() * freq / SAMPLE_RATE)
                // Square wave buzz
                val square = if (sin(2.0 * Math.PI * phase) > 0.0) 1.0 else -1.0
                effectMix += square * env * 0.4
                effectEnvelopes["laser"] = env * 0.96f
            } else {
                activeEffects.remove("laser")
                effectEnvelopes.remove("laser")
            }
        }

        // Explosion effect: Low pitch brown-like noise decay
        activeEffects["explosion"]?.let { sampleOffset ->
            val env = effectEnvelopes["explosion"] ?: 0f
            if (env > 0.01f) {
                activeEffects["explosion"] = sampleOffset + 1024

                val t = sampleOffset.toDouble() / SAMPLE_RATE
                val noise = Random.nextDouble() * 2.0 - 1.0
                
                // Simple low pass filter simulation via rolling average (low frequency rumble)
                val rumble = noise * Math.exp(-4.0 * t)
                
                effectMix += rumble * env * 0.9
                effectEnvelopes["explosion"] = env * 0.98f
            } else {
                activeEffects.remove("explosion")
                effectEnvelopes.remove("explosion")
            }
        }

        // Special charge effect: Rising pitch arpeggio
        activeEffects["charge"]?.let { sampleOffset ->
            val env = effectEnvelopes["charge"] ?: 0f
            if (env > 0.01f) {
                activeEffects["charge"] = sampleOffset + 1024

                val t = sampleOffset.toDouble() / SAMPLE_RATE
                // Sweep frequency UP
                val freq = 300.0 + 1200.0 * (sampleOffset.toDouble() / (SAMPLE_RATE * 1.5))
                effectMix += sin(2.0 * Math.PI * freq * t) * env * 0.6
                
                effectEnvelopes["charge"] = env * 0.99f
            } else {
                activeEffects.remove("charge")
                effectEnvelopes.remove("charge")
            }
        }

        return effectMix
    }
}
