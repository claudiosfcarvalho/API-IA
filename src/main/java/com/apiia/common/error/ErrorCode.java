package com.apiia.common.error;

/**
 * Enumeração de códigos de erro padrão da aplicação.
 * 
 * Cada código representa uma classe de erro específica que pode ocorrer
 * durante operações de IA local, transcrição ou síntese de fala.
 * 
 * @author API-IA
 * @version 1.0
 */
public enum ErrorCode {
    /** Requisição inválida ou malformada */
    INVALID_REQUEST,
    
    // Erro de IA Local (Multimodal)
    /** Requisição multimodal vazia */
    MULTIMODAL_EMPTY_REQUEST,
    /** Tipo de imagem não suportado */
    MULTIMODAL_INVALID_IMAGE_TYPE,
    /** Tipo de áudio não suportado */
    MULTIMODAL_INVALID_AUDIO_TYPE,
    /** Imagem excede tamanho máximo permitido */
    MULTIMODAL_IMAGE_TOO_LARGE,
    /** Áudio excede tamanho máximo permitido */
    MULTIMODAL_AUDIO_TOO_LARGE,
    /** Timeout ao processar requisição de IA local */
    IA_LOCAL_TIMEOUT,
    /** Serviço de IA local indisponível */
    IA_LOCAL_UNAVAILABLE,
    /** Erro interno no processamento de IA local */
    IA_LOCAL_INTERNAL_ERROR,
    
    // Erro de Transcrição
    /** Arquivo de áudio não encontrado */
    TRANSCRIPTION_FILE_NOT_FOUND,
    /** Acesso negado ao arquivo de áudio */
    TRANSCRIPTION_FORBIDDEN_PATH,
    /** Arquivo de áudio excede tamanho máximo */
    TRANSCRIPTION_FILE_TOO_LARGE,
    /** Timeout ao transcrever áudio */
    TRANSCRIPTION_TIMEOUT,
    /** Serviço de transcrição indisponível */
    TRANSCRIPTION_SERVICE_UNAVAILABLE,
    /** Arquivo de transcrição não encontrado para download */
    TRANSCRIPTION_DOWNLOAD_NOT_FOUND,
    /** Falha ao escrever resultado de transcrição */
    TRANSCRIPTION_OUTPUT_WRITE_FAILED,
    /** Erro interno no serviço de transcrição */
    TRANSCRIPTION_INTERNAL_ERROR,
    
    // Erro de TTS (Text-to-Speech)
    /** Requisição de síntese de fala inválida */
    TTS_INVALID_REQUEST,
    /** Formato de saída não suportado */
    TTS_INVALID_FORMAT,
    /** Timeout ao sintetizar fala */
    TTS_TIMEOUT,
    /** Serviço de TTS indisponível */
    TTS_UNAVAILABLE,
    /** Erro interno no serviço de TTS */
    TTS_INTERNAL_ERROR
}
