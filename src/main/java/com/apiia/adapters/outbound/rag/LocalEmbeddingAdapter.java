package com.apiia.adapters.outbound.rag;

import com.apiia.application.ports.out.rag.EmbeddingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação local de embeddings usando hash-based shallow embeddings.
 * 
 * NOTA EDUCACIONAL: Em produção, use modelos como:
 * - Ollama com embedding model
 * - Hugging Face all-minilm-l6-v2
 * - OpenAI embeddings API
 * 
 * Esta implementação demonstra o conceito: cada palavra gera um hash,
 * que é normalizado em um vetor dimensional.
 * 
 * @author API-IA
 * @version 1.0
 */
@Component
public class LocalEmbeddingAdapter implements EmbeddingPort {
    private static final Logger logger = LoggerFactory.getLogger(LocalEmbeddingAdapter.class);
    private static final int EMBEDDING_DIMENSION = 384; // tamanho do vetor

    @Override
    public double[] embed(String text) {
        logger.debug("Gerando embedding para texto de {} caracteres", text.length());
        
        // Normaliza e limpa o texto
        String normalized = text.toLowerCase().replaceAll("[^a-zá-ú0-9 ]", "");
        String[] words = normalized.split("\\s+");

        // Inicializa array de embedding com zeros
        double[] embedding = new double[EMBEDDING_DIMENSION];

        // Para cada palavra, gera um hash e o distribui no array
        for (String word : words) {
            if (!word.isEmpty()) {
                int hash = hashWord(word);
                // Distribui o valor do hash em múltiplas posições
                int startIdx = Math.abs(hash) % EMBEDDING_DIMENSION;
                for (int i = 0; i < Math.min(5, EMBEDDING_DIMENSION); i++) {
                    int idx = (startIdx + i) % EMBEDDING_DIMENSION;
                    embedding[idx] += hashValueToDouble(word, i);
                }
            }
        }

        // Normaliza o vetor (magnitude = 1)
        normalizeVector(embedding);

        logger.debug("Embedding gerado com sucesso");
        return embedding;
    }

    @Override
    public List<double[]> embedBatch(List<String> texts) {
        logger.debug("Gerando embeddings em batch para {} textos", texts.size());
        List<double[]> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(embed(text));
        }
        return embeddings;
    }

    /**
     * Gera hash simples de uma palavra usando MD5.
     */
    private int hashWord(String word) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(word.getBytes());
            // Converte primeiro byte para int
            return (hash[0] & 0xFF);
        } catch (NoSuchAlgorithmException e) {
            // Fallback simples
            return word.hashCode();
        }
    }

    /**
     * Converte hash para valor double no intervalo [0, 1].
     */
    private double hashValueToDouble(String word, int seed) {
        int hash = word.hashCode() ^ seed;
        return Math.abs(hash % 1000) / 1000.0;
    }

    /**
     * Normaliza um vetor para magnitude = 1.
     */
    private void normalizeVector(double[] vector) {
        double magnitude = 0.0;
        for (double v : vector) {
            magnitude += v * v;
        }
        magnitude = Math.sqrt(magnitude);

        if (magnitude > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = vector[i] / magnitude;
            }
        }
    }
}
