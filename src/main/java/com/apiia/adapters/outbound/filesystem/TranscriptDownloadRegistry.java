package com.apiia.adapters.outbound.filesystem;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TranscriptDownloadRegistry {

    private final Map<String, Path> index = new ConcurrentHashMap<>();

    public void put(String transcriptId, Path filePath) {
        index.put(transcriptId, filePath);
    }

    public Optional<Path> get(String transcriptId) {
        return Optional.ofNullable(index.get(transcriptId));
    }
}
