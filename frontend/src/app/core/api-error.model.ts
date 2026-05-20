/**
 * Interface para bloco de erro funcional em resposta RFC 7807.
 * 
 * Representa a perspectiva do usuário sobre o erro, com código e mensagem legível.
 */
export interface FunctionalApiError {
  /** Código de erro padronizado (ex: "IA_LOCAL_TIMEOUT") */
  code: string;
  /** Mensagem funcional em português para exibição ao usuário */
  message: string;
}

/**
 * Interface para bloco de erro técnico em resposta RFC 7807.
 * 
 * Contém informações técnicas para debugging e rastreamento de requisições.
 */
export interface TechnicalApiError {
  /** ID único de correlação para rastreamento em logs */
  correlationId?: string;
  /** Timestamp (ISO 8601) quando o erro ocorreu */
  timestamp?: string;
  /** Nome da classe de exceção Java que causou o erro */
  exception?: string;
  /** Detalhes técnicos adicionais em formato mapa */
  details?: Record<string, unknown>;
  /** Caminho HTTP da requisição que falhou */
  path?: string;
  /** Status HTTP da resposta (ex: 500, 504) */
  status?: number;
  /** Título padrão do status HTTP */
  title?: string;
  /** URI do tipo de problema RFC 7807 */
  type?: string;
}

/**
 * Interface para resposta de erro conforme RFC 7807 (Problem Details for HTTP APIs).
 * 
 * Padrão IEEE para erro em APIs HTTP com suporte a detalhes funcionais e técnicos.
 */
export interface Rfc7807Problem {
  /** URI identificando o tipo de problema */
  type?: string;
  /** Título descritivo do problema */
  title?: string;
  /** Status HTTP numérico */
  status?: number;
  /** Descrição legível do problema específico */
  detail?: string;
  /** URI identificando a instância específica do problema */
  instance?: string;
  /** Código de erro (duplicado de functional.code para compatibilidade) */
  code?: string;
  /** Bloco de erro funcional (perspectiva do usuário) */
  functional?: FunctionalApiError;
  /** Bloco de erro técnico (perspectiva de debugging) */
  technical?: TechnicalApiError;
}

/**
 * Interface para erro normalizado dentro da aplicação frontend.
 * 
 * Unifica o formato de erros provenientes da API em um modelo consistente
 * para tratamento e exibição na interface do usuário.
 */
export interface NormalizedApiError {
  /** Erro funcional com código e mensagem */
  functional: FunctionalApiError;
  /** Erro técnico com informações para debugging */
  technical: TechnicalApiError;
  /** Resposta bruta original da API */
  raw?: unknown;
}