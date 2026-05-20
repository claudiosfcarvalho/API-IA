/**
 * Bootstrap da aplicação Angular.
 * 
 * Ponto de entrada que carrega o componente raiz (AppComponent)
 * com as configurações da aplicação (appConfig).
 * 
 * @author API-IA Frontend
 * @version 1.0
 */
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

/**
 * Inicia a aplicação Angular.
 * 
 * Realiza bootstrap com AppComponent e configurações padrão.
 * Em caso de erro, loga a exceção no console do navegador.
 */
bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
