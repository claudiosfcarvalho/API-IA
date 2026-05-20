import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Interface para resposta de IA local.
 * 
 * Contém a resposta do modelo LLM junto com métricas de processamento e tokens.
 */
export interface IaResponse {
  /** ID de correlação para rastreamento */
  correlationId: string;
  /** Modelo utilizado para processamento */
  model: string;
  /** Resposta gerada pelo modelo */
  answer: string;
  /** Métricas de processamento e tokens */
  metrics: {
    inputTokens: number;
    outputTokens: number;
    totalTokens: number;
    estimatedCost: number;
    processingTimeMs: number;
    ollamaTotalDurationMs: number;
    ollamaPromptEvalCount: number;
    ollamaEvalCount: number;
  };
}

/**
 * Interface para resposta de transcrição.
 * 
 * Contém o texto transcrito, segmentos de fala, identificação de falantes e métodos de download.
 */
export interface TranscriptionResponse {
  /** ID de correlação para rastreamento */
  correlationId: string;
  /** Modelo utilizado para transcrição */
  model: string;
  /** Idioma detectado ou utilizado */
  language: string;
  /** Número de falantes identificados */
  numSpeakers: number;
  /** ID único da transcrição para download posterior */
  transcriptId: string;
  /** URL relativa para download do arquivo de transcrição */
  downloadUrl: string;
  /** Caminho do arquivo salvo no servidor */
  outputFile: string;
  /** Texto completo transcrito */
  transcript: string;
  /** Segmentos da transcrição com timestamps e identificação de falante */
  segments: Array<{ speaker: string; startMs: number; endMs: number; text: string }>;
  /** Métricas de processamento */
  metrics: { processingTimeMs: number };
}

/**
 * Serviço de cliente HTTP para comunicação com API-IA backend.
 * 
 * Fornece métodos para:
 * - Consultar IA local com texto ou multimodal (imagem + áudio + texto)
 * - Transcrever áudio via upload de arquivo
 * - Sintetizar fala (Text-to-Speech)
 * - Baixar transcrições processadas
 * 
 * @author API-IA Frontend
 * @version 1.0
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly base = environment.apiBaseUrl;

  /**
   * Construtor com injeção de HttpClient.
   * 
   * @param http cliente HTTP do Angular
   */
  constructor(private readonly http: HttpClient) {}

  /**
   * Consulta IA local com texto simples.
   * 
   * @param input texto a processar
   * @param model modelo a utilizar (opcional)
   * @returns observable com resposta de IA
   */
  askText(input: string, model?: string): Observable<IaResponse> {
    return this.http.post<IaResponse>(`${this.base}/api/ia-local`, { input, model });
  }

  /**
   * Consulta IA local com entrada multimodal (imagem + áudio + texto).
   * 
   * @param input texto da requisição (opcional)
   * @param model modelo a utilizar
   * @param imageFile arquivo de imagem (opcional)
   * @param audioFile arquivo de áudio (opcional)
   * @returns observable com resposta multimodal
   */
  askMultimodal(input: string, model: string, imageFile?: File, audioFile?: File): Observable<IaResponse> {
    const body = new FormData();
    if (input?.trim()) {
      body.append('input', input.trim());
    }
    if (model?.trim()) {
      body.append('model', model.trim());
    }
    if (imageFile) {
      body.append('imageFile', imageFile);
    }
    if (audioFile) {
      body.append('audioFile', audioFile);
    }
    return this.http.post<IaResponse>(`${this.base}/api/ia-local/multimodal`, body);
  }

  /**
   * Faz upload de arquivo de áudio e o transcreve.
   * 
   * @param file arquivo de áudio para transcrever
   * @param language idioma esperado (opcional)
   * @param numSpeakers número de falantes (opcional)
   * @param model modelo de transcrição a utilizar (opcional)
   * @param diarize se deve identificar falantes
   * @returns observable com resposta de transcrição
   */
  transcribeUpload(file: File, language?: string, numSpeakers?: number, model?: string, diarize = true): Observable<TranscriptionResponse> {
    const body = new FormData();
    body.append('file', file);

    let params = new HttpParams().set('diarize', `${diarize}`);
    if (language?.trim()) {
      params = params.set('language', language.trim());
    }
    if (numSpeakers && numSpeakers > 0) {
      params = params.set('numSpeakers', `${numSpeakers}`);
    }
    if (model?.trim()) {
      params = params.set('model', model.trim());
    }

    return this.http.post<TranscriptionResponse>(`${this.base}/api/transcricao-audio/upload`, body, { params });
  }

  /**
   * Faz download de arquivo de transcrição já processado.
   * 
   * @param downloadUrl URL relativa do arquivo no servidor
   * @returns observable com blob do arquivo
   */
  downloadTranscript(downloadUrl: string): Observable<Blob> {
    return this.http.get(`${this.base}${downloadUrl}`, { responseType: 'blob' });
  }

  /**
   * Sintetiza texto em áudio (Text-to-Speech).
   * 
   * @param text texto a sintetizar
   * @param voice voz/falante a utilizar (opcional)
   * @param language idioma do texto (opcional)
   * @param format formato de áudio ("wav" ou "mp3")
   * @returns observable com blob do arquivo de áudio
   */
  textToSpeech(text: string, voice?: string, language?: string, format: 'wav' | 'mp3' = 'wav'): Observable<Blob> {
    return this.http.post(`${this.base}/api/tts`, { text, voice, language, format }, { responseType: 'blob' });
  }

  /**
   * Lista todas as vozes disponíveis para TTS.
   * 
   * @returns observable com array de nomes de vozes
   */
  listVoices(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/api/tts/voices`);
  }
}
