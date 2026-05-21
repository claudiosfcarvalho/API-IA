package com.apiia.adapters.outbound.rag;

final class EmbeddingJsonCodec {

    private EmbeddingJsonCodec() {
    }

    static String encode(double[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    static double[] decode(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return new double[0];
        }
        String normalized = json.trim();
        if (normalized.startsWith("[")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("]")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            return new double[0];
        }

        String[] parts = normalized.split(",");
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Double.parseDouble(parts[i].trim());
        }
        return values;
    }
}
