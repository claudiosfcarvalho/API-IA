/**
 * Configurações de ambiente da aplicação frontend.
 * 
 * Este arquivo contém configurações específicas do ambiente de desenvolvimento.
 * Para produção, usar environment.prod.ts.
 * 
 * @author API-IA Frontend
 * @version 1.0
 */
export const environment = {
  /** Indicador se está em modo produção (falso para desenvolvimento) */
  production: false,
  /** URL base da API backend */
  apiBaseUrl: 'http://localhost:8080',
  /** Tamanho máximo permitido para upload de imagens (10MB) */
  maxImageBytes: 10 * 1024 * 1024,
  /** Tamanho máximo permitido para upload de áudio (200MB) */
  maxAudioBytes: 200 * 1024 * 1024
};
