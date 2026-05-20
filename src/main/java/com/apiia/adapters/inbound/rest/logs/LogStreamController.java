package com.apiia.adapters.inbound.rest.logs;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogStreamController {
    private static final Path PRIMARY_LOG_PATH = Path.of("logs/backend.log");

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs() throws IOException {
        SseEmitter emitter = new SseEmitter(0L); // sem timeout
        Path logPath = resolveLogPath();
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new FileReader(logPath.toFile()))) {
                String line;
                // Skip to end of file for tail -f behavior
                while (reader.readLine() != null) {}
                while (true) {
                    line = reader.readLine();
                    if (line != null) {
                        emitter.send(line);
                    } else {
                        Thread.sleep(500);
                    }
                }
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }, "log-stream-thread").start();
        return emitter;
    }

    @GetMapping(value = "/tail", produces = MediaType.APPLICATION_JSON_VALUE)
    public LogTailResponse tailLogs(
            @RequestParam(name = "fromLine", defaultValue = "0") long fromLine,
            @RequestParam(name = "limit", defaultValue = "200") int limit
    ) throws IOException {
        Path logPath = resolveLogPath();
        long safeFromLine = Math.max(0L, fromLine);
        int safeLimit = Math.min(Math.max(limit, 1), 1000);

        if (!Files.exists(logPath)) {
            return new LogTailResponse(
                    logPath.toString(),
                    0L,
                    0L,
                    List.of("[log] Arquivo de log ainda nao foi criado: " + logPath)
            );
        }

        List<String> lines = new ArrayList<>();
        long currentLine = 0L;

        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (currentLine >= safeFromLine && lines.size() < safeLimit) {
                    lines.add(line);
                }
                currentLine++;
            }
        }

        return new LogTailResponse(
                logPath.toString(),
                currentLine,
                safeFromLine + lines.size(),
                lines
        );
    }

    private Path resolveLogPath() {
        List<Path> candidates = List.of(
                PRIMARY_LOG_PATH,
                Path.of("../logs/backend.log"),
                Path.of(System.getProperty("user.dir"), "logs", "backend.log")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        return PRIMARY_LOG_PATH;
    }

    public record LogTailResponse(
            String path,
            long totalLines,
            long nextLine,
            List<String> lines
    ) {
    }
}
