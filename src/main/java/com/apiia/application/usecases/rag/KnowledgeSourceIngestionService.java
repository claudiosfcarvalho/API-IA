package com.apiia.application.usecases.rag;

import com.apiia.application.ports.out.rag.DocumentIndexPort;
import com.apiia.application.ports.out.rag.VectorStorePort;
import com.apiia.common.error.AppException;
import com.apiia.common.error.ErrorCode;
import com.apiia.config.properties.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class KnowledgeSourceIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSourceIngestionService.class);

    private final DefaultIndexDocumentsUseCase indexDocumentsUseCase;
    private final DocumentIndexPort documentIndexPort;
    private final VectorStorePort vectorStorePort;
    private final AppProperties appProperties;

    public KnowledgeSourceIngestionService(DefaultIndexDocumentsUseCase indexDocumentsUseCase,
                                           DocumentIndexPort documentIndexPort,
                                           VectorStorePort vectorStorePort,
                                           AppProperties appProperties) {
        this.indexDocumentsUseCase = indexDocumentsUseCase;
        this.documentIndexPort = documentIndexPort;
        this.vectorStorePort = vectorStorePort;
        this.appProperties = appProperties;
    }

    @Transactional
    public KnowledgeIngestionResult ingestAndPersist(String title,
                                                     String markdown,
                                                     String category,
                                                     String source,
                                                     String requestedFileName) {
        String safeTitle = (title == null || title.isBlank()) ? "Documento" : title.trim();
        String safeMarkdown = markdown == null ? "" : markdown.trim();
        if (safeMarkdown.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                    "contentMarkdown e obrigatorio");
        }

        Path knowledgeRoot = resolveKnowledgeRoot();
        ensureDirectory(knowledgeRoot);

        String fileName = normalizeMarkdownFileName(requestedFileName, safeTitle);
        Path target = knowledgeRoot.resolve(fileName).normalize();

        if (!target.startsWith(knowledgeRoot)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                    "Nome de arquivo invalido");
        }

        try {
            Files.writeString(target, safeMarkdown, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao salvar markdown na knowledge-source");
        }

        String relativeSource = "knowledge-source/" + fileName;
        String effectiveSource = (source == null || source.isBlank()) ? relativeSource : source.trim();
        return ingestInternal(safeTitle, safeMarkdown, effectiveSource, category, target);
    }

    @Transactional
    public KnowledgeIngestionResult ingestFromFile(Path markdownPath) {
        try {
            String markdown = Files.readString(markdownPath, StandardCharsets.UTF_8);
            String fileName = markdownPath.getFileName().toString();
            String title = fileName.replaceFirst("(?i)\\.md$", "");

            Path knowledgeRoot = resolveKnowledgeRoot();
            String relative = knowledgeRoot.toAbsolutePath().normalize().relativize(markdownPath.toAbsolutePath().normalize())
                    .toString().replace('\\', '/');
            String source = "knowledge-source/" + relative;

            return ingestInternal(title, markdown, source, appProperties.getRag().getDefaultCategory(), markdownPath);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao ler markdown da knowledge-source");
        }
    }

    @Transactional
    public KnowledgeBootstrapSummary bootstrapAll() {
        Path knowledgeRoot = resolveKnowledgeRoot();
        ensureDirectory(knowledgeRoot);

        int scanned = 0;
        int indexed = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        try (Stream<Path> stream = Files.walk(knowledgeRoot)) {
            var files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                    .toList();

            for (Path file : files) {
                scanned++;
                try {
                    KnowledgeIngestionResult result = ingestFromFile(file);
                    if (result.skipped()) {
                        skipped++;
                    } else if (result.updated()) {
                        updated++;
                    } else {
                        indexed++;
                    }
                } catch (Exception ex) {
                    failed++;
                    log.error("falha ao indexar markdown {}", file, ex);
                }
            }
        } catch (IOException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao varrer knowledge-source");
        }

        return new KnowledgeBootstrapSummary(scanned, indexed, updated, skipped, failed);
    }

    private KnowledgeIngestionResult ingestInternal(String title,
                                                    String markdown,
                                                    String source,
                                                    String category,
                                                    Path physicalPath) {
        String effectiveCategory = (category == null || category.isBlank())
                ? appProperties.getRag().getDefaultCategory()
                : category.trim();

        String newHash = sha256(markdown);
        Optional<com.apiia.domain.rag.Document> existing = documentIndexPort.findBySource(source);
        boolean updated = false;

        if (existing.isPresent()) {
            String existingHash = sha256(existing.get().getContent());
            if (existingHash.equals(newHash)) {
                return new KnowledgeIngestionResult(
                        source,
                        existing.get().getId(),
                        existing.get().getChunks().size(),
                        true,
                        false,
                        physicalPath.toString(),
                        "Documento sem alteracoes; indexacao ignorada"
                );
            }

            vectorStorePort.deleteByDocumentId(existing.get().getId());
            documentIndexPort.delete(existing.get().getId());
            updated = true;
        }

        IndexDocumentResult result = indexDocumentsUseCase.execute(new IndexDocumentCommand(
                title,
                markdown,
                source,
                effectiveCategory
        ));

        return new KnowledgeIngestionResult(
                source,
                result.documentId(),
                result.chunksCreated(),
                false,
                updated,
                physicalPath.toString(),
                result.message()
        );
    }

    private Path resolveKnowledgeRoot() {
        return Paths.get(appProperties.getRag().getKnowledgeSourceDir()).toAbsolutePath().normalize();
    }

    private static void ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.TRANSCRIPTION_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao preparar diretorio de knowledge-source");
        }
    }

    private static String normalizeMarkdownFileName(String requestedFileName, String title) {
        String fileName;
        if (requestedFileName != null && !requestedFileName.isBlank()) {
            fileName = requestedFileName.trim().replace('\\', '_').replace('/', '_');
        } else {
            fileName = slugify(title) + "-" + System.currentTimeMillis() + ".md";
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".md")) {
            fileName += ".md";
        }
        return fileName;
    }

    private static String slugify(String value) {
        String slug = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "documento" : slug;
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nao disponivel", ex);
        }
    }
}
