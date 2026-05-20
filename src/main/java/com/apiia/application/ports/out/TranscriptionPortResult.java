package com.apiia.application.ports.out;

import java.util.List;

public record TranscriptionPortResult(
        String model,
        String language,
        List<TranscriptionSegmentRaw> segments
) {
}
