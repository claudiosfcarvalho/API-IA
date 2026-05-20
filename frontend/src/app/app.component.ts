import { CommonModule } from '@angular/common';
import { Component, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, BackendDashboardMetricsResponse, BackendLogTailResponse, IaResponse, TranscriptionResponse } from './core/api.service';
import { NormalizedApiError, Rfc7807Problem, TechnicalApiError } from './core/api-error.model';
import { environment } from '../environments/environment';

/**
 * Componente raiz da aplicação Angular.
 * 
 * Gerencia três abas principais:
 * - Assistant: consulta de IA local com suporte a entrada multimodal (imagem + áudio + texto)
 * - Transcription: upload e transcrição de arquivos de áudio
 * - TTS: síntese de texto em fala
 * 
 * Inclui painel DEV DEBUG para visualização de detalhes técnicos de erros em modo desenvolvimento.
 * 
 * @author API-IA Frontend
 * @version 1.0
 */
@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  /** Aba ativa atualmente */
  activeTab: 'assistant' | 'transcription' | 'tts' | 'rag' = 'assistant';

  // === Propriedades de Assistant (IA Local) ===
  /** Texto de entrada para consulta de IA */
  assistantInput = '';
  /** Modelo selecionado para processamento */
  assistantModel = 'llama3.2';
  /** Arquivo de imagem selecionado */
  assistantImageFile?: File;
  /** Arquivo de áudio selecionado */
  assistantAudioFile?: File;
  /** Indicador de carregamento */
  assistantLoading = false;
  /** Resultado da resposta de IA */
  assistantResult?: IaResponse;

  // === Propriedades de Transcrição ===
  /** Arquivo de áudio para transcrição */
  transcriptionFile?: File;
  /** Idioma do áudio */
  transcriptionLanguage = 'pt';
  /** Modelo de transcrição */
  transcriptionModel = 'large-v3';
  /** Indicador de carregamento */
  transcriptionLoading = false;
  /** Resultado da transcrição */
  transcriptionResult?: TranscriptionResponse;
  /** URL do áudio original para playback */
  originalAudioUrl?: string;

  // === Propriedades de TTS (Síntese de Fala) ===
  /** Texto a sintetizar */
  ttsText = '';
  /** Voz/falante selecionado */
  ttsVoice = 'default';
  /** Idioma do texto */
  ttsLanguage = 'pt-BR';
  /** Formato de saída (WAV ou MP3) */
  ttsFormat: 'wav' | 'mp3' = 'wav';
  /** Indicador de carregamento */
  ttsLoading = false;
  /** URL do áudio sintetizado para playback */
  ttsAudioUrl?: string;
  /** Lista de vozes disponíveis */
  ttsVoices: string[] = [];

  // === Propriedades de UI e Debug ===
  /** Mensagem de notificação ao usuário */
  toastMessage = '';
  /** Tipo de mensagem de notificação */
  toastType: 'success' | 'error' = 'success';
  /** Indicador de visibilidade do painel DEV DEBUG */
  showDevDebug = false;
  /** Último erro capturado para exibição */
  latestError?: NormalizedApiError;
  /** Linhas de log ao vivo do backend */
  liveLogs: string[] = [];
  /** Cursor da próxima linha de log */
  liveLogsNextLine = 0;
  /** Polling de logs em tempo real */
  logsTimer?: ReturnType<typeof setInterval>;
  /** Evita sobreposição de chamadas de logs */
  logsLoading = false;
  /** Snapshot de métricas do backend */
  backendMetrics?: BackendDashboardMetricsResponse;
  /** Snapshot de métricas coletadas no frontend */
  frontendMetrics: {
    logicalCores: number;
    deviceMemoryGb: number;
    jsHeapUsedMb: number;
    jsHeapLimitMb: number;
    networkType: string;
    downlinkMbps: number;
    rttMs: number;
    avgApiLatencyMs: number;
    dashboardRoundTripMs: number;
    approxNetworkTransferKb: number;
  } = {
    logicalCores: 0,
    deviceMemoryGb: 0,
    jsHeapUsedMb: 0,
    jsHeapLimitMb: 0,
    networkType: '-',
    downlinkMbps: 0,
    rttMs: 0,
    avgApiLatencyMs: 0,
    dashboardRoundTripMs: 0,
    approxNetworkTransferKb: 0
  };
  /** Controla polling do dashboard */
  dashboardTimer?: ReturnType<typeof setInterval>;
  /** Evita sobreposição de chamadas de métricas */
  dashboardLoading = false;

  // === Propriedades de RAG ===
  /** Query para RAG */
  ragQuery = '';
  /** Fase RAG selecionada */
  ragPhase: 1 | 2 | 3 | 4 = 1;
  /** Resultado RAG */
  ragResult?: any;
  /** Indicador de carregamento RAG */
  ragLoading = false;

  // === Rastreamento de Método Usado ===
  /** Método usado na última resposta */
  lastMethodUsed?: string;
  /** Detalhes de execução da última resposta */
  lastExecutionDetails?: string;
  /** Tempo de execução da última operação */
  lastExecutionTimeMs?: number;

  // === Constantes ===
  /** Tamanho máximo permitido para imagens em bytes */
  readonly maxImageBytes = environment.maxImageBytes;
  /** Tamanho máximo permitido para áudio em bytes */
  readonly maxAudioBytes = environment.maxAudioBytes;
  /** Indicador se está em modo desenvolvimento */
  readonly devMode = !environment.production;

  /**
   * Construtor com injeção de serviço de API.
   * 
   * Carrega lista de vozes disponíveis ao inicializar.
   * 
   * @param api serviço de comunicação com backend
   */
  constructor(private readonly api: ApiService) {
    this.api.listVoices().subscribe({
      next: voices => (this.ttsVoices = voices),
      error: () => (this.ttsVoices = ['default'])
    });
  }

  ngOnDestroy(): void {
    this.stopDebugRealtime();
    if (this.dashboardTimer) {
      clearInterval(this.dashboardTimer);
      this.dashboardTimer = undefined;
    }
  }

  /**
   * Seleciona a aba ativa.
   * 
   * @param tab identificador da aba
   */
  selectTab(tab: 'assistant' | 'transcription' | 'tts' | 'rag'): void {
    this.activeTab = tab;
  }

  /**
   * Processa seleção de arquivo de imagem para IA local.
   * 
   * Valida tamanho e atribui ao campo de imagem.
   * 
   * @param event evento de seleção de arquivo
   */
  onAssistantImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    if (file.size > this.maxImageBytes) {
      this.showToast('Imagem excede o limite de 10MB.', 'error');
      input.value = '';
      return;
    }
    this.assistantImageFile = file;
  }

  /**
   * Processa seleção de arquivo de áudio para IA local.
   * 
   * Valida tamanho e atribui ao campo de áudio.
   * 
   * @param event evento de seleção de arquivo
   */
  onAssistantAudioSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    if (file.size > this.maxAudioBytes) {
      this.showToast('Audio excede o limite de 200MB.', 'error');
      input.value = '';
      return;
    }
    this.assistantAudioFile = file;
  }

  sendAssistantRequest(): void {
    const hasText = this.assistantInput.trim().length > 0;
    if (!hasText && !this.assistantImageFile && !this.assistantAudioFile) {
      this.showToast('Informe texto, imagem ou audio.', 'error');
      return;
    }

    this.assistantLoading = true;
    this.assistantResult = undefined;

    this.api
      .askMultimodal(this.assistantInput, this.assistantModel, this.assistantImageFile, this.assistantAudioFile)
      .subscribe({
        next: response => {
          this.assistantResult = response;
          this.latestError = undefined;
          this.showToast('Resposta gerada com sucesso.', 'success');
        },
        error: error => {
          this.handleApiError(error);
        },
        complete: () => {
          this.assistantLoading = false;
        }
      });
  }

  onTranscriptionAudioSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    if (file.size > this.maxAudioBytes) {
      this.showToast('Audio excede o limite de 200MB.', 'error');
      input.value = '';
      return;
    }
    this.transcriptionFile = file;
    this.originalAudioUrl = URL.createObjectURL(file);
  }

  sendTranscriptionRequest(): void {
    if (!this.transcriptionFile) {
      this.showToast('Selecione um arquivo de audio.', 'error');
      return;
    }

    this.transcriptionLoading = true;
    this.transcriptionResult = undefined;

    this.api
      .transcribeUpload(this.transcriptionFile, this.transcriptionLanguage, 0, this.transcriptionModel, true)
      .subscribe({
        next: response => {
          this.transcriptionResult = response;
          this.latestError = undefined;
          this.showToast('Transcricao concluida.', 'success');
        },
        error: error => {
          this.handleApiError(error);
        },
        complete: () => {
          this.transcriptionLoading = false;
        }
      });
  }

  downloadTranscript(): void {
    if (!this.transcriptionResult?.downloadUrl) {
      this.showToast('Nenhuma transcricao disponivel para download.', 'error');
      return;
    }

    this.api.downloadTranscript(this.transcriptionResult.downloadUrl).subscribe({
      next: blob => {
        const fileName = `transcricao-${this.transcriptionResult?.transcriptId ?? 'arquivo'}.txt`;
        this.downloadBlob(blob, fileName);
        this.latestError = undefined;
        this.showToast('Download iniciado.', 'success');
      },
      error: error => this.handleApiError(error)
    });
  }

  generateTts(): void {
    if (!this.ttsText.trim()) {
      this.showToast('Informe um texto para gerar audio.', 'error');
      return;
    }

    this.ttsLoading = true;
    if (this.ttsAudioUrl) {
      URL.revokeObjectURL(this.ttsAudioUrl);
      this.ttsAudioUrl = undefined;
    }

    this.api.textToSpeech(this.ttsText, this.ttsVoice, this.ttsLanguage, this.ttsFormat).subscribe({
      next: blob => {
        this.ttsAudioUrl = URL.createObjectURL(blob);
        this.latestError = undefined;
        this.showToast('Audio gerado com sucesso.', 'success');
      },
      error: error => this.handleApiError(error),
      complete: () => {
        this.ttsLoading = false;
      }
    });
  }

  downloadTtsAudio(): void {
    if (!this.ttsAudioUrl) {
      this.showToast('Nenhum audio gerado ainda.', 'error');
      return;
    }

    fetch(this.ttsAudioUrl)
      .then(response => response.blob())
      .then(blob => this.downloadBlob(blob, `tts-output.${this.ttsFormat}`));
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    this.toastMessage = message;
    this.toastType = type;
    setTimeout(() => {
      if (this.toastMessage === message) {
        this.toastMessage = '';
      }
    }, 3500);
  }


  toggleDevDebug(): void {
    this.showDevDebug = !this.showDevDebug;
    if (this.showDevDebug) {
      this.startDebugRealtime();
    } else {
      this.stopDebugRealtime();
    }
  }

  private startDebugRealtime(): void {
    this.refreshDashboardMetrics();
    this.dashboardTimer = setInterval(() => this.refreshDashboardMetrics(), 3000);

    this.startLogPolling();
  }

  private stopDebugRealtime(): void {
    this.stopLogPolling();
    if (this.dashboardTimer) {
      clearInterval(this.dashboardTimer);
      this.dashboardTimer = undefined;
    }
    this.dashboardLoading = false;
  }

  private startLogPolling(): void {
    this.liveLogs = [];
    this.liveLogsNextLine = 0;
    this.fetchLogsChunk(400, true);
    this.logsTimer = setInterval(() => this.fetchLogsChunk(200, false), 1500);
  }

  private stopLogPolling(): void {
    if (this.logsTimer) {
      clearInterval(this.logsTimer);
      this.logsTimer = undefined;
    }
    this.logsLoading = false;
  }

  private refreshDashboardMetrics(): void {
    if (!this.showDevDebug) {
      return;
    }

    if (this.dashboardLoading) {
      return;
    }

    this.dashboardLoading = true;
    const startedAt = performance.now();

    this.api.getBackendDashboardMetrics().subscribe({
      next: (metrics) => {
        this.backendMetrics = metrics;
        this.frontendMetrics.dashboardRoundTripMs = Math.round(performance.now() - startedAt);
        this.collectFrontendMetrics();
      },
      error: () => {
        this.collectFrontendMetrics();
        this.dashboardLoading = false;
      },
      complete: () => {
        this.dashboardLoading = false;
      }
    });
  }

  private fetchLogsChunk(limit: number, catchUp: boolean): void {
    if (!this.showDevDebug || this.logsLoading) {
      return;
    }

    this.logsLoading = true;
    this.api.getBackendLogTail(this.liveLogsNextLine, limit).subscribe({
      next: (response: BackendLogTailResponse) => {
        this.liveLogsNextLine = response.nextLine;
        if (response.lines?.length) {
          this.liveLogs = [...this.liveLogs, ...response.lines].slice(-1200);
        }
        if (catchUp && response.lines?.length === limit) {
          this.logsLoading = false;
          this.fetchLogsChunk(limit, true);
          return;
        }
      },
      error: () => {
        this.logsLoading = false;
      },
      complete: () => {
        this.logsLoading = false;
      }
    });
  }

  private collectFrontendMetrics(): void {
    const navAny = navigator as any;
    const connection = navAny.connection || navAny.mozConnection || navAny.webkitConnection;
    const perfAny = performance as any;
    const memory = perfAny.memory;

    const resourceEntries = performance
      .getEntriesByType('resource')
      .filter((entry) => (entry as PerformanceResourceTiming).name?.includes('/api/')) as PerformanceResourceTiming[];

    const recentEntries = resourceEntries.slice(-10);
    const avgApiLatency = recentEntries.length
      ? recentEntries.reduce((acc, entry) => acc + entry.duration, 0) / recentEntries.length
      : 0;
    const transferKb = recentEntries.length
      ? recentEntries.reduce((acc, entry) => acc + (entry.transferSize || 0), 0) / 1024
      : 0;

    this.frontendMetrics = {
      logicalCores: navigator.hardwareConcurrency || 0,
      deviceMemoryGb: navAny.deviceMemory || 0,
      jsHeapUsedMb: memory ? Math.round(memory.usedJSHeapSize / (1024 * 1024)) : 0,
      jsHeapLimitMb: memory ? Math.round(memory.jsHeapSizeLimit / (1024 * 1024)) : 0,
      networkType: connection?.effectiveType || '-',
      downlinkMbps: connection?.downlink || 0,
      rttMs: connection?.rtt || 0,
      avgApiLatencyMs: Math.round(avgApiLatency),
      dashboardRoundTripMs: this.frontendMetrics.dashboardRoundTripMs,
      approxNetworkTransferKb: Math.round(transferKb)
    };
  }

  formatUptime(ms?: number): string {
    if (!ms || ms <= 0) {
      return '-';
    }
    const totalSeconds = Math.floor(ms / 1000);
    const h = Math.floor(totalSeconds / 3600);
    const m = Math.floor((totalSeconds % 3600) / 60);
    const s = totalSeconds % 60;
    return `${h}h ${m}m ${s}s`;
  }

  devDebugSnapshot(): string {
    if (!this.latestError) {
      return 'Nenhum erro registrado nesta sessao.';
    }
    return JSON.stringify(this.latestError, null, 2);
  }

  private handleApiError(error: any): void {
    const normalized = this.normalizeApiError(error);
    this.latestError = normalized;
    this.showToast(normalized.functional.message, 'error');
  }

  private normalizeApiError(error: any): NormalizedApiError {
    const rawBody = error?.error;
    const problem = this.asProblem(rawBody);

    const functional = {
      code: problem?.functional?.code || problem?.code || rawBody?.error?.code || 'UNEXPECTED_ERROR',
      message:
        problem?.functional?.message ||
        problem?.detail ||
        rawBody?.error?.message ||
        error?.message ||
        'Falha inesperada.'
    };

    const technical: TechnicalApiError = {
      correlationId: problem?.technical?.correlationId || rawBody?.correlationId,
      timestamp: problem?.technical?.timestamp,
      exception: problem?.technical?.exception,
      details: problem?.technical?.details || rawBody?.error?.details,
      path: problem?.instance,
      status: problem?.status || error?.status,
      title: problem?.title,
      type: problem?.type
    };

    return {
      functional,
      technical,
      raw: rawBody ?? error
    };
  }

  private asProblem(payload: any): Rfc7807Problem | undefined {
    if (!payload || typeof payload !== 'object') {
      return undefined;
    }
    if ('functional' in payload || 'detail' in payload || 'title' in payload || 'status' in payload) {
      return payload as Rfc7807Problem;
    }
    return undefined;
  }

  // ==================== RAG Methods ====================

  executeRag(): void {
    if (!this.ragQuery.trim()) {
      this.showToast('Informe uma pergunta para RAG.', 'error');
      return;
    }

    this.ragLoading = true;
    this.ragResult = undefined;
    this.lastMethodUsed = undefined;
    this.lastExecutionDetails = undefined;

    const startTime = performance.now();

    const ragObservable = this.ragPhase === 1
      ? this.api.ragPhase1(this.ragQuery, 'MotoGP', 3)
      : this.ragPhase === 2
      ? this.api.ragPhase2(this.ragQuery, 'MotoGP', 5, 'llama2', 0.7)
      : this.ragPhase === 3
      ? this.api.ragPhase3(this.ragQuery, 'search_motogp', 'llama2', 0.5)
      : this.api.ragPhase4(this.ragQuery, 'llama2', 0.8, 5);

    ragObservable.subscribe({
      next: (response) => {
        this.ragResult = response;
        this.lastMethodUsed = response.method || `FASE ${this.ragPhase}`;
        this.lastExecutionDetails = response.executionDetails;
        this.lastExecutionTimeMs = Math.round(performance.now() - startTime);
        this.latestError = undefined;
        this.showToast(`Fase ${this.ragPhase} executada com sucesso em ${this.lastExecutionTimeMs}ms`, 'success');
      },
      error: (error) => {
        this.handleApiError(error);
      },
      complete: () => {
        this.ragLoading = false;
      }
    });
  }
}
