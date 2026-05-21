package com.apiia.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Propriedades globais da aplicação.
 * 
 * Agrupa todas as propriedades configuráveis da aplicação através do arquivo
 * application.yml, separadas por domínio (LLM, Transcrição, Multimodal, TTS, Web).
 * 
 * @author API-IA
 * @version 1.0
 */
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @Valid
    @NotNull
    private LlmProperties llm = new LlmProperties();

    @Valid
    @NotNull
    private TranscriptionProperties transcription = new TranscriptionProperties();

    @Valid
    @NotNull
    private MultimodalProperties multimodal = new MultimodalProperties();

    @Valid
    @NotNull
    private TtsProperties tts = new TtsProperties();

    @Valid
    @NotNull
    private WebProperties web = new WebProperties();

    @Valid
    @NotNull
    private RagProperties rag = new RagProperties();

    /**
     * Retorna as propriedades do serviço LLM (Ollama).
     * 
     * @return propriedades de configuração do LLM
     */
    public LlmProperties getLlm() {
        return llm;
    }

    /**
     * Define as propriedades do serviço LLM.
     * 
     * @param llm propriedades do LLM
     */
    public void setLlm(LlmProperties llm) {
        this.llm = llm;
    }

    /**
     * Retorna as propriedades do serviço de transcrição.
     * 
     * @return propriedades de configuração da transcrição
     */
    public TranscriptionProperties getTranscription() {
        return transcription;
    }

    /**
     * Define as propriedades do serviço de transcrição.
     * 
     * @param transcription propriedades da transcrição
     */
    public void setTranscription(TranscriptionProperties transcription) {
        this.transcription = transcription;
    }

    /**
     * Retorna as propriedades de requisições multimodais.
     * 
     * @return propriedades de configuração multimodal
     */
    public MultimodalProperties getMultimodal() {
        return multimodal;
    }

    /**
     * Define as propriedades multimodais.
     * 
     * @param multimodal propriedades multimodal
     */
    public void setMultimodal(MultimodalProperties multimodal) {
        this.multimodal = multimodal;
    }

    /**
     * Retorna as propriedades do serviço TTS (síntese de fala).
     * 
     * @return propriedades de configuração do TTS
     */
    public TtsProperties getTts() {
        return tts;
    }

    public void setTts(TtsProperties tts) {
        this.tts = tts;
    }

    public WebProperties getWeb() {
        return web;
    }

    public void setWeb(WebProperties web) {
        this.web = web;
    }

    public RagProperties getRag() {
        return rag;
    }

    public void setRag(RagProperties rag) {
        this.rag = rag;
    }

    public static class LlmProperties {

        @NotBlank
        private String baseUrl;

        @NotBlank
        private String defaultModel;

        @NotNull
        private Duration timeout = Duration.ofSeconds(30);

        @Min(1)
        private int maxInputChars = 20_000;

        @NotNull
        private CostProperties cost = new CostProperties();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaxInputChars() {
            return maxInputChars;
        }

        public void setMaxInputChars(int maxInputChars) {
            this.maxInputChars = maxInputChars;
        }

        public CostProperties getCost() {
            return cost;
        }

        public void setCost(CostProperties cost) {
            this.cost = cost;
        }
    }

    public static class CostProperties {

        private double inputPer1K;
        private double outputPer1K;

        public double getInputPer1K() {
            return inputPer1K;
        }

        public void setInputPer1K(double inputPer1K) {
            this.inputPer1K = inputPer1K;
        }

        public double getOutputPer1K() {
            return outputPer1K;
        }

        public void setOutputPer1K(double outputPer1K) {
            this.outputPer1K = outputPer1K;
        }
    }

    public static class TranscriptionProperties {

        private boolean enabled = true;

        @NotBlank
        private String baseUrl;

        @NotBlank
        private String defaultModel;

        @NotBlank
        private String allowedDir;

        @NotBlank
        private String outputDir;

        @NotNull
        private Duration timeout = Duration.ofSeconds(120);

        @NotNull
        private Duration processingTimeout = Duration.ofMinutes(30);

        @Min(1)
        private long maxFileBytes = 209_715_200L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }

        public String getAllowedDir() {
            return allowedDir;
        }

        public void setAllowedDir(String allowedDir) {
            this.allowedDir = allowedDir;
        }

        public String getOutputDir() {
            return outputDir;
        }

        public void setOutputDir(String outputDir) {
            this.outputDir = outputDir;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Duration getProcessingTimeout() {
            return processingTimeout;
        }

        public void setProcessingTimeout(Duration processingTimeout) {
            this.processingTimeout = processingTimeout;
        }

        public long getMaxFileBytes() {
            return maxFileBytes;
        }

        public void setMaxFileBytes(long maxFileBytes) {
            this.maxFileBytes = maxFileBytes;
        }
    }

    public static class MultimodalProperties {

        @Min(1)
        private long maxImageBytes = 10_485_760L;

        @Min(1)
        private long maxAudioBytes = 209_715_200L;

        @NotBlank
        private String allowedImageTypes = "image/jpeg,image/png,image/webp";

        @NotBlank
        private String allowedAudioTypes = "audio/mpeg,audio/wav,audio/x-wav,audio/mp4,audio/x-m4a";

        @NotBlank
        private String visionModel = "llava:7b";

        public long getMaxImageBytes() {
            return maxImageBytes;
        }

        public void setMaxImageBytes(long maxImageBytes) {
            this.maxImageBytes = maxImageBytes;
        }

        public long getMaxAudioBytes() {
            return maxAudioBytes;
        }

        public void setMaxAudioBytes(long maxAudioBytes) {
            this.maxAudioBytes = maxAudioBytes;
        }

        public String getAllowedImageTypes() {
            return allowedImageTypes;
        }

        public void setAllowedImageTypes(String allowedImageTypes) {
            this.allowedImageTypes = allowedImageTypes;
        }

        public String getAllowedAudioTypes() {
            return allowedAudioTypes;
        }

        public void setAllowedAudioTypes(String allowedAudioTypes) {
            this.allowedAudioTypes = allowedAudioTypes;
        }

        public String getVisionModel() {
            return visionModel;
        }

        public void setVisionModel(String visionModel) {
            this.visionModel = visionModel;
        }
    }

    public static class TtsProperties {

        @NotNull
        private Duration timeout = Duration.ofSeconds(60);

        @Min(1)
        private int maxTextChars = 10_000;

        @NotBlank
        private String defaultLanguage = "pt-BR";

        @NotBlank
        private String defaultVoice = "default";

        @NotBlank
        private String defaultFormat = "wav";

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaxTextChars() {
            return maxTextChars;
        }

        public void setMaxTextChars(int maxTextChars) {
            this.maxTextChars = maxTextChars;
        }

        public String getDefaultLanguage() {
            return defaultLanguage;
        }

        public void setDefaultLanguage(String defaultLanguage) {
            this.defaultLanguage = defaultLanguage;
        }

        public String getDefaultVoice() {
            return defaultVoice;
        }

        public void setDefaultVoice(String defaultVoice) {
            this.defaultVoice = defaultVoice;
        }

        public String getDefaultFormat() {
            return defaultFormat;
        }

        public void setDefaultFormat(String defaultFormat) {
            this.defaultFormat = defaultFormat;
        }
    }

    public static class WebProperties {

        @NotBlank
        private String corsAllowedOrigins = "http://localhost:4200";

        public String getCorsAllowedOrigins() {
            return corsAllowedOrigins;
        }

        public void setCorsAllowedOrigins(String corsAllowedOrigins) {
            this.corsAllowedOrigins = corsAllowedOrigins;
        }
    }

    public static class RagProperties {

        @NotBlank
        private String knowledgeSourceDir = "knowledge-source";

        @NotBlank
        private String defaultCategory = "general";

        private boolean bootstrapOnStartup = true;

        public String getKnowledgeSourceDir() {
            return knowledgeSourceDir;
        }

        public void setKnowledgeSourceDir(String knowledgeSourceDir) {
            this.knowledgeSourceDir = knowledgeSourceDir;
        }

        public String getDefaultCategory() {
            return defaultCategory;
        }

        public void setDefaultCategory(String defaultCategory) {
            this.defaultCategory = defaultCategory;
        }

        public boolean isBootstrapOnStartup() {
            return bootstrapOnStartup;
        }

        public void setBootstrapOnStartup(boolean bootstrapOnStartup) {
            this.bootstrapOnStartup = bootstrapOnStartup;
        }
    }
}
