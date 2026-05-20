package com.apiia.adapters.outbound.tts;

import com.apiia.application.ports.out.TtsPort;
import com.apiia.application.usecases.tts.TtsCommand;
import com.apiia.application.usecases.tts.TtsResult;
import com.apiia.config.properties.AppProperties;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class LocalSyntheticTtsAdapter implements TtsPort {

    private final AppProperties appProperties;

    public LocalSyntheticTtsAdapter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public TtsResult synthesize(TtsCommand command) {
        String format = command.format() == null || command.format().isBlank()
                ? appProperties.getTts().getDefaultFormat()
                : command.format().toLowerCase();

        byte[] wavBytes = buildWavFromText(command.text());

        // For local/offline mode we always synthesize PCM WAV and expose mp3 as compatibility mode.
        return new TtsResult(
                wavBytes,
                format,
                command.voice() == null || command.voice().isBlank() ? appProperties.getTts().getDefaultVoice() : command.voice(),
                command.language() == null || command.language().isBlank() ? appProperties.getTts().getDefaultLanguage() : command.language()
        );
    }

    @Override
    public List<String> voices() {
        return List.of("default", "narrator", "neutral");
    }

    private byte[] buildWavFromText(String text) {
        try {
            float sampleRate = 16_000f;
            int durationMs = Math.max(500, Math.min(8000, text.length() * 90));
            int samples = (int) ((durationMs / 1000.0) * sampleRate);

            byte[] pcm = new byte[samples * 2];
            double freq = 220.0;
            for (int i = 0; i < samples; i++) {
                double angle = 2.0 * Math.PI * i * freq / sampleRate;
                short value = (short) (Math.sin(angle) * 6000);
                pcm[i * 2] = (byte) (value & 0xFF);
                pcm[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
            }

            AudioFormat audioFormat = new AudioFormat(sampleRate, 16, 1, true, false);
            AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(pcm), audioFormat, samples);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao sintetizar audio local", ex);
        }
    }
}
