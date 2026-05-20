package com.apiia.domain.rag;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade representando um documento para indexação em RAG.
 * Especializado em MotoGP, mas pode conter qualquer conteúdo.
 * 
 * @author API-IA
 * @version 1.0
 */
public class Document {
    private final String id;
    private final String title;
    private final String content;
    private final String source; // URL, arquivo, etc
    private final String category; // ex: "MotoGP", "Técnico", "Histórico"
    private final LocalDateTime createdAt;
    private final List<Chunk> chunks;

    private Document(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.title = builder.title;
        this.content = builder.content;
        this.source = builder.source;
        this.category = builder.category;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.chunks = new ArrayList<>();
    }

    public static class Builder {
        private String id;
        private String title;
        private String content;
        private String source;
        private String category;
        private LocalDateTime createdAt;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Document build() {
            return new Document(this);
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSource() {
        return source;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Chunk> getChunks() {
        return chunks;
    }

    public void addChunk(Chunk chunk) {
        this.chunks.add(chunk);
    }

    @Override
    public String toString() {
        return "Document{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
