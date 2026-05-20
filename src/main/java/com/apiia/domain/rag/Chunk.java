package com.apiia.domain.rag;

import java.util.Arrays;
import java.util.UUID;

/**
 * Fragmento (chunk) de um documento para indexação em RAG.
 * Cada chunk é independente e pode ser recuperado via similaridade de embedding.
 * 
 * @author API-IA
 * @version 1.0
 */
public class Chunk {
    private final String id;
    private final String documentId;
    private final String text;
    private final int index; // ordem do chunk dentro do documento
    private double[] embedding; // vetor de embeddings (lazy loaded)
    private double similarity; // similaridade com query (preenchida em retrieval)

    private Chunk(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.documentId = builder.documentId;
        this.text = builder.text;
        this.index = builder.index;
        this.embedding = builder.embedding;
        this.similarity = 0.0;
    }

    public static class Builder {
        private String id;
        private String documentId;
        private String text;
        private int index;
        private double[] embedding;

        public Builder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder index(int index) {
            this.index = index;
            return this;
        }

        public Builder embedding(double[] embedding) {
            this.embedding = embedding;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Chunk build() {
            return new Chunk(this);
        }
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getText() {
        return text;
    }

    public int getIndex() {
        return index;
    }

    public double[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(double[] embedding) {
        this.embedding = embedding;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    /**
     * Calcula similaridade de cosseno entre dois embeddings.
     * Fórmula: (A · B) / (||A|| * ||B||)
     */
    public static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "id='" + id + '\'' +
                ", documentId='" + documentId + '\'' +
                ", index=" + index +
                ", textLength=" + text.length() +
                ", similarity=" + String.format("%.4f", similarity) +
                '}';
    }
}
