/**
 * Configurações globais da aplicação Angular.
 * 
 * Define providers de:
 * - Zone.js para detecção de mudanças otimizada
 * - Router para navegação entre páginas
 * - HttpClient com interceptor customizado de correlation ID
 * 
 * @author API-IA Frontend
 * @version 1.0
 */
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { correlationInterceptor } from './core/correlation.interceptor';

/**
 * Configuração da aplicação Angular.
 * 
 * Combines:
 * - Detecção de mudanças otimizada com zone.js
 * - Roteador para navegação
 * - HttpClient com interceptor de correlation ID para rastreamento
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // Otimiza detecção de mudanças coalescing eventos rapidamente
    provideZoneChangeDetection({ eventCoalescing: true }),
    // Roteador da aplicação
    provideRouter(routes),
    // HttpClient com interceptor customizado para adicionar X-Correlation-Id
    provideHttpClient(withInterceptors([correlationInterceptor]))
  ]
};
