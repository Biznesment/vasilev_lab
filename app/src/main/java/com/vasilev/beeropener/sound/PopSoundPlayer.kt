package com.vasilev.beeropener.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Программный синтез звука открытия бутылки («чпок» крышки + шипение пены)
 * через стандартный Android AudioTrack без внешних аудиофайлов.
 */
object PopSoundPlayer {

    fun playOpenSound() {
        Thread {
            runCatching {
                val sampleRate = 22050
                val durationMs = 360
                val numSamples = (sampleRate * durationMs) / 1000
                val samples = ShortArray(numSamples)

                var prevNoise = 0.0
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate

                    // Короткий щелчок срыва металлической крышки
                    val pop = if (t < 0.04) {
                        val freq = 390.0 - 230.0 * (t / 0.04)
                        sin(2.0 * PI * freq * t) * exp(-t * 80.0) * 0.9
                    } else {
                        0.0
                    }

                    // Шипение выходящего газа и пены («пшшш»)
                    val fizz = if (t >= 0.018) {
                        val dt = t - 0.018
                        val rawNoise = Random.nextDouble(-1.0, 1.0)
                        val highPass = rawNoise - 0.55 * prevNoise
                        prevNoise = rawNoise
                        val attack = (dt / 0.035).coerceIn(0.0, 1.0)
                        val decay = exp(-dt * 6.8)
                        highPass * attack * decay * 0.42
                    } else {
                        0.0
                    }

                    val mixed = (pop + fizz).coerceIn(-1.0, 1.0)
                    samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep((durationMs + 70).toLong())
                track.stop()
                track.release()
            }
        }.start()
    }
}
