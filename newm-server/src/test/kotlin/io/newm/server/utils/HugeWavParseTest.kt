package io.newm.server.utils

import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.ktx.propertiesFromResource
import org.apache.tika.Tika
import org.jaudiotagger.audio.AudioFileIO
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties

class HugeWavParseTest {
    private val mimeTypes: Properties by lazy {
        propertiesFromResource("audio-mime-types.properties")
    }

    @Test
    @Disabled
    fun testParseHugeWav() {
        val file = File("/home/westbam/Downloads/romeo-2.wav")
        val type = Tika().detect(file)
        val ext =
            mimeTypes.getProperty(type)
                ?: throw HttpUnprocessableEntityException("Unsupported media type: $type")

        // enforce duration
        val header = AudioFileIO.readAs(file, ext).audioHeader
        val duration = header.trackLength
        val minDuration = 60
        if (duration < minDuration) throw HttpUnprocessableEntityException("Duration is too short: $duration secs")

        // enforce sampling rate
        val sampleRate = header.sampleRateAsNumber
        val minSampleRate = 44100
        if (sampleRate < minSampleRate) throw HttpUnprocessableEntityException("Sample rate is too low: $sampleRate Hz")
    }
}
