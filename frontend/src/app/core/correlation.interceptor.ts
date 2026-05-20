import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Gera um ID de correlação único para rastreamento de requisições.
 * 
 * Utiliza a API cripto nativa do navegador (Web Crypto API) se disponível,
 * caso contrário gera um ID fallback usando timestamp + número aleatório.
 * 
 * @returns string com UUID ou fallback ID
 */
function randomCorrelationId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `cid-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

/**
 * Interceptor HTTP para propagação de correlation ID.
 * 
 * Adiciona um header X-Correlation-Id único em cada requisição HTTP,
 * permitindo rastreamento de requisições em logs distribuídos.
 * 
 * @param req requisição HTTP
 * @param next próximo interceptor na cadeia
 * @returns observable com a requisição processada
 */
export const correlationInterceptor: HttpInterceptorFn = (req, next) => {
  const correlationId = randomCorrelationId();
  const cloned = req.clone({
    setHeaders: {
      'X-Correlation-Id': correlationId
    }
  });
  return next(cloned);
};
