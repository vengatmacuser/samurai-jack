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
    @Volatile var isLowHealthHeartbeatActive = false

    // Sound effect trigger states
    // Stores: soundName -> Time index or decay samples left
    private val activeEffects = ConcurrentHashMap<String, Int>()
    private val effectEnvelopes = ConcurrentHashMap<String, Float>()

    fun start(context: Context) {
        if (isRunning) return
        isRunning = true

        try {
            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.soundtrack).apply {
                isLooping = true
                setVolume(0.8f, 0.8f) // Increased volume for emulator audibility
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
            
            // Build AudioTrack using modern Builder on API 23+ for emulator compatibility
            audioTrack = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_GAME)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufSize.coerceAtLeast(4096))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize.coerceAtLeast(4096),
                    AudioTrack.MODE_STREAM
                )
            }
            
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

    fun stopMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop/release MediaPlayer", e)
        }
        mediaPlayer = null
    }

    fun playMusic(context: Context) {
        if (mediaPlayer != null) return
        try {
            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.soundtrack).apply {
                isLooping = true
                setVolume(0.8f, 0.8f) // Increased volume for emulator audibility
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play background music", e)
        }
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
