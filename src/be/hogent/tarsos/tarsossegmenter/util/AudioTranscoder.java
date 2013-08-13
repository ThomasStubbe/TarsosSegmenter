/**
 *
 * Tarsos is developed by Joren Six at The Royal Academy of Fine Arts & Royal
 * Conservatory, University College Ghent, Hoogpoort 64, 9000 Ghent - Belgium
 * 
*
 */
package be.hogent.tarsos.tarsossegmenter.util;

import be.hogent.tarsos.tarsossegmenter.util.configuration.ConfKey;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration;
import be.hogent.tarsos.tarsossegmenter.util.io.FileUtils;
import be.hogent.tarsos.transcoder.Attributes;
import be.hogent.tarsos.transcoder.DefaultAttributes;
import be.hogent.tarsos.transcoder.Transcoder;
import be.hogent.tarsos.transcoder.ffmpeg.EncoderException;

import java.io.File;

/**
 * Transcodes audio. Uses ffmpeg: a vast number of formats are supported. A
 * compiled ffmpeg binary is provided for Windows and Linux (32bit). If support
 * on another platform is required check the <a
 * href="http://www.sauronsoftware.it/projects/jave/manual.php">documentation of
 * JAVE</a> or disable transcoding (trough configuration) and perform it
 * manually .
 *
 * @author Joren Six
 */
public final class AudioTranscoder {

    // makes sure no instances of AudioTranscoder are created.
    private AudioTranscoder() {
    }
    ;

	/**
	 * Defines the target codec: signed 16 bit little endian pcm.
	 */
	public static final DefaultAttributes TARGET_ENCODING = DefaultAttributes.valueOf(Configuration.get(ConfKey.transcoded_audio_to));

    /**
     * Transcode the source file to target using the requested number of
     * channels and sampling rate.
     *
     * @param source The path of the source file.
     * @param target The path of the target file. A stereo stream can be down
     * mixed to a mono stream. Converting a mono stream to a stereo stream
     * results in a file with two channels with the same data.
     * @throws EncoderException If FFMPEG fails to transcode the audio an
     * UnsupportedAudioFileException is generated.
     * @throws IllegalArgumentException if the source file can not be read or
     * the target file is not writable.
     */
    public static void transcode(final String source, final String target) throws EncoderException {
        transcode(source, target, TARGET_ENCODING.getAttributes());
    }

    /**
     * Transcode the source file to target using the requested number of
     * channels and sampling rate.
     *
     * @param source The path of the source file.
     * @param target The path of the target file.
     * @param attributes Defines the attributes of the audio e.g. The sampling
     * rate the target file should have; The number of channels target should
     * have. A stereo stream can be down mixed to a mono stream.
     * @throws EncoderException When the transcoder yields an error.
     */
    public static void transcode(final String source, final String target, final Attributes attributes)
            throws EncoderException {
        final File sourceFile = new File(source);
        final File targetFile = new File(target);

        // if transcoding is enabled transcode
        if (Configuration.getBoolean(ConfKey.transcode_audio)) {
            Transcoder.transcode(sourceFile, targetFile, attributes);
        } else {
            // if transcoding is disabled: copy the audio
            FileUtils.cp(source, target);
        }
    }

    public static boolean transcodingRequired(final String transcodedPath) {
        final boolean required;
        if (FileUtils.exists(transcodedPath)) {
            if (Configuration.getBoolean(ConfKey.transcode_check_format)) {
                required = Transcoder.transcodingRequired(transcodedPath, TARGET_ENCODING.getAttributes());
            } else {
                required = false;
            }
        } else {
            required = true;
        }

        return required;
    }
}
