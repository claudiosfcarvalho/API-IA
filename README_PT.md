# API-IA - Aplicação Integrada de Inteligência Artificial

Documentação em Português da aplicação API-IA.

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Arquitetura](#arquitetura)
3. [Tecnologias](#tecnologias)
4. [Pré-requisitos](#pré-requisitos)
5. [Inicialização](#inicialização)
6. [Estrutura de Diretórios](#estrutura-de-diretórios)
7. [Documentação Técnica](#documentação-técnica)
8. [APIs](#apis)
9. [Tratamento de Erros](#tratamento-de-erros)
10. [Resolução de Problemas](#resolução-de-problemas)

---

## Visão Geral

API-IA é uma aplicação full-stack que integra três serviços de inteligência artificial:

- **IA Local (Ollama)**: Processamento de linguagem natural com modelos de IA local
- **Transcrição (WhisperX)**: Transcrição de áudio para texto com identificação de falantes
- **Síntese de Fala (TTS)**: Conversão de texto para áudio

A aplicação está estruturada em:
- **Backend**: Spring Boot 3.5.0 com Java 25
- **Frontend**: Angular standalone com TypeScript
- **Infraestrutura**: Docker Compose para orquestração de serviços

---

## Alterações Recentes do Repositório

### Transcrição de Áudio (WhisperX)
- Timeout de processamento longo controlado por `processingTimeout` (atual: `30m` no `application.yml`).
- Cancelamento explícito da execução assíncrona quando o timeout é atingido, evitando chamadas penduradas.
- Chamada HTTP via `curl` com `--connect-timeout` e `--max-time` baseada nas propriedades da aplicação.
- Mapeamento de timeout do `curl` (exit code `28`) para erro funcional `TRANSCRIPTION_TIMEOUT` com status `504`.

### RAG Local (Backend + Frontend)
- Endpoints RAG disponíveis em `POST /api/rag/documents`, `POST /api/rag/phase1`, `POST /api/rag/phase2`, `POST /api/rag/phase3`, `POST /api/rag/phase4`.
- Fluxo educacional em 4 fases (RAG puro, RAG + LLM, MCP tools, Agentic loop).
- Integração no frontend via `api.service.ts` para todas as fases e indexação de documentos.
- Persistência RAG em H2 (em memória), substituindo armazenamento volátil apenas em maps locais.
- Bootstrap automático de arquivos markdown da pasta `knowledge-source/` ao subir a API.
- Reprocessamento por hash para arquivos markdown alterados (evita duplicidade e mantém consistência).
- Novo endpoint `POST /api/rag/context` para salvar markdown na knowledge-source e indexar imediatamente no RAG.

### Configuração Operacional
- Timeout de transcrição curto (`timeout`) configurado para `120s`.
- Timeout de processamento longo (`processingTimeout`) configurado para `30m`.
- Novos endpoints de transcrição assíncrona com progresso: `POST /api/transcricao-audio/upload/async` e `GET /api/transcricao-audio/progresso/{jobId}`.

---

## Plano de Evolução

1. Persistir documentos e chunks RAG em H2 (modo em memória).
2. Introduzir pasta `knowledge-source/` para arquivos markdown como fonte canônica.
3. Carregar automaticamente os markdowns da `knowledge-source/` no startup da API.
4. Criar ingestão incremental via API para salvar markdown no disco e indexar no banco em memória.
5. Manter compatibilidade dos endpoints atuais de RAG (`/documents`, `phase1..phase4`).
6. Adicionar endpoint de progresso de transcrição por `jobId` com status e percentual.
7. Incluir `progressPercent`, `elapsedMs`, `processedSeconds`, `totalSeconds` e `estimated` na resposta.
8. Disponibilizar stream opcional de progresso (SSE) para atualização em tempo real no frontend.
9. Exibir barra de progresso no frontend para arquivos de áudio longos.
10. Cobrir bootstrap, ingestão e progresso com testes automatizados.
11. Documentar rotas novas, payloads e fluxo de operação no README.
12. Adicionar reprocessamento de documentos markdown alterados (detecção por hash) para manter índice e arquivos sempre consistentes.

---

## Arquitetura

### Estrutura de Camadas (Backend)

```
com.apiia
├── adapters           # Adaptadores de entrada/saída
│   ├── inbound       # Controllers REST
│   └── outbound      # Integrações com serviços externos
├── application        # Lógica de aplicação
│   ├── ports         # Interfaces de portas (Clean Architecture)
│   ├── usecases      # Casos de uso
├── config            # Configurações de infraestrutura
├── common            # Utilitários compartilhados
│   ├── error         # Tratamento de erros RFC 7807
│   └── correlation   # Rastreamento de requisições
└── domain            # Entidades de domínio
```

### Arquitetura Hexagonal (Clean Architecture)

```
           [Frontend Angular]
                  |
           [REST Controllers]
                  |
          [Use Cases / Services]
                  |
        [Ports: LLM, Transcription, TTS]
                  |
         [Adapters: Ollama, WhisperX, etc]
```

---

## Tecnologias

### Backend
- **Java 25**: Linguagem de programação
- **Spring Boot 3.5.0**: Framework web
- **Spring Web 6.2.7**: MVC e REST
- **Tomcat 10.1.41**: Servlet container
- **Maven 3.9.10**: Build automation
- **Resilience4j**: Padrões de resiliência (retry, circuit-breaker, timeout)
- **Jackson**: Serialização JSON

### Frontend
- **Angular 19**: Framework web
- **TypeScript 5.6+**: Linguagem compilada
- **npm**: Package manager
- **RxJS**: Programação reativa

### Infraestrutura
- **Docker Compose**: Orquestração de contêineres
- **Ollama**: Servidor LLM local
- **WhisperX**: Servidor de transcrição
- **PostgreSQL**: Banco de dados (opcional em futuras versões)

---

## Pré-requisitos

### Windows
- Docker Desktop (com WSL2 ou Hyper-V)
- Maven 3.9+
- Java 25 JDK
- Node.js 20+ e npm 10+
- Git Bash ou PowerShell Core

### Linux/macOS
- Docker e Docker Compose
- Maven 3.9+
- Java 25 JDK
- Node.js 20+ e npm 10+
- Bash ou Zsh

### Verificar Instalações
```bash
# Java
java -version

# Maven
mvn --version

# Node.js
node --version
npm --version

# Docker
docker --version
docker-compose --version
```

---

## Inicialização

### Windows
```bash
# Abrir prompt de comando (cmd) ou PowerShell na raiz do projeto
cd scripts
.\start.bat

# Para parar:
.\stop.bat
```

### Linux/macOS
```bash
# Terminal bash/zsh
cd scripts
chmod +x start.sh stop.sh
./start.sh

# Para parar:
./stop.sh
```

### Processo de Inicialização
1. Docker Compose levanta Ollama (porta 11434) e WhisperX (porta 9000)
2. Script aguarda disponibilidade de ambos os serviços
3. Backend Spring Boot inicia na porta 8080
4. Script aguarda health check do backend
5. Frontend Angular inicia na porta 4200
6. Exibe URLs de acesso

### URLs Disponíveis
- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- Health Backend: http://localhost:8080/actuator/health
- Health WhisperX: http://localhost:9000/health

---

## Estrutura de Diretórios

```
API-IA/
├── src/
│   ├── main/
│   │   ├── java/com/apiia/           # Código-fonte Java
│   │   │   ├── adapters/             # Controllers e adaptadores
│   │   │   ├── application/          # Casos de uso
│   │   │   ├── config/               # Configurações Spring
│   │   │   ├── common/               # Código compartilhado
│   │   │   └── domain/               # Entidades de domínio
│   │   └── resources/
│   │       ├── application.yml       # Configurações aplicação
│   │       └── logback-spring.xml    # Configuração de logging
│   └── test/
│       └── java/                     # Testes unitários e integração
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/                 # Serviços core (API, error handling)
│   │   │   ├── app.component.ts      # Componente raiz
│   │   │   ├── app.config.ts         # Configuração do app
│   │   │   └── app.routes.ts         # Rotas da aplicação
│   │   ├── environments/             # Configurações de ambiente
│   │   └── main.ts                   # Bootstrap da aplicação
│   ├── angular.json                  # Configuração Angular
│   ├── tsconfig.json                 # Configuração TypeScript
│   └── package.json                  # Dependências npm
├── scripts/
│   ├── start.bat / start.sh           # Inicializar aplicação
│   └── stop.bat / stop.sh             # Parar aplicação
├── docker-compose.yml                # Orquestração de serviços
└── pom.xml                            # Dependências Maven

```

---

## Documentação Técnica

### Java - Tratamento de Erros (RFC 7807)

A aplicação implementa o padrão RFC 7807 (Problem Details for HTTP APIs) para retornar erros estruturados.

#### Interfaces Principais

**`ApiBusinessException.java`**
- Contrato para exceções de negócio
- Métodos: `getErrorCode()`, `getStatus()`, `getFunctionalMessage()`, `getTechnicalDetails()`

**`AppException.java`**
- Implementação padrão de exceção de negócio
- Encapsula código de erro, status HTTP, mensagem e detalhes

**`GlobalExceptionHandler.java`**
- Manipulador global de exceções
- Converte todas as exceções em respostas RFC 7807
- Separa erro funcional (para usuário) de erro técnico (para debug)

**`ErrorCode.enum`**
- Enumeração de códigos de erro padrão
- Exemplos: `IA_LOCAL_TIMEOUT`, `TRANSCRIPTION_FILE_TOO_LARGE`, `TTS_UNAVAILABLE`

#### Exemplo de Resposta de Erro

```json
{
  "type": "about:blank",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "Modelo de IA local não respondeu no tempo esperado",
  "instance": "http://localhost:8080/api/ia-local",
  "code": "IA_LOCAL_TIMEOUT",
  "functional": {
    "code": "IA_LOCAL_TIMEOUT",
    "message": "Timeout ao processar requisição de IA local"
  },
  "technical": {
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-05-20T14:30:45+00:00",
    "path": "/api/ia-local",
    "status": 504,
    "title": "Gateway Timeout",
    "type": "about:blank",
    "exception": "io.github.resilience4j.timelimiter.TimeLimiterException",
    "details": {
      "timeoutSeconds": 120,
      "model": "llama3.2"
    }
  }
}
```

### TypeScript - Modelos de Erro

**`api-error.model.ts`**
- `FunctionalApiError`: Código e mensagem para usuário
- `TechnicalApiError`: Detalhes técnicos com correlation ID
- `Rfc7807Problem`: Resposta completa RFC 7807
- `NormalizedApiError`: Modelo normalizado para frontend

**`api.service.ts`**
- Serviço HTTP centralizado
- Métodos principais:
  - `askText(input, model)`: Consulta IA com texto
  - `askMultimodal(input, model, image, audio)`: Consulta multimodal
  - `transcribeUpload(file, language, speakers, model)`: Upload e transcrição
  - `textToSpeech(text, voice, language, format)`: Síntese de fala
  - `listVoices()`: Lista vozes disponíveis

**`correlation.interceptor.ts`**
- Interceptor HTTP que adiciona header `X-Correlation-Id`
- Permite rastreamento de requisições em logs distribuídos

### Configuração - application.yml

O arquivo `application.yml` centraliza todas as configurações:

**Serviço LLM (Ollama)**
```yaml
app.llm:
  baseUrl: http://localhost:11434
  defaultModel: llama3.2
  timeout: 120s              # Aumentado de 30s para evitar timeouts
```

**Serviço de Transcrição**
```yaml
app.transcription:
  baseUrl: http://localhost:9000
  defaultModel: large-v3
  timeout: 120s
  maxFileBytes: 209715200    # 200MB
```

**Upload de Arquivos**
```yaml
spring.servlet.multipart:
  max-file-size: 200MB
  max-request-size: 210MB
```

**Políticas de Resiliência**
- Retry: 2 tentativas com espera
- Circuit Breaker: Abre após 50% de falhas
- Time Limiter: Timeout de 120s (LLM) e 60s (TTS)
- Bulkhead: Máximo 20 chamadas simultâneas ao LLM

---

## APIs

### 1. IA Local (Ollama)

#### POST /api/ia-local
Processa texto com modelo de IA local.

**Request (JSON ou Texto Simples)**
```json
{
  "input": "Como você está?",
  "model": "llama3.2"
}
```

**Response (200 OK)**
```json
{
  "correlationId": "abc123",
  "model": "llama3.2",
  "answer": "Estou funcionando muito bem!",
  "metrics": {
    "inputTokens": 5,
    "outputTokens": 7,
    "totalTokens": 12,
    "processingTimeMs": 1250
  }
}
```

#### POST /api/ia-local/multimodal
Processa entrada multimodal (texto + imagem + áudio).

**Request (Form Data)**
- `input`: Texto (opcional)
- `model`: Modelo a usar (obrigatório)
- `imageFile`: Arquivo de imagem (opcional)
- `audioFile`: Arquivo de áudio (opcional)

**Response**: Mesmo formato que IA Local

### 2. Transcrição (WhisperX)

#### POST /api/transcricao-audio/upload
Faz upload e transcreve arquivo de áudio.

**Request (Form Data)**
- `file`: Arquivo de áudio (obrigatório, máx 200MB)
- `language`: Código de idioma, ex: "pt" (opcional)
- `numSpeakers`: Número esperado de falantes (opcional)
- `model`: Modelo de transcrição (opcional)
- `diarize`: Se deve identificar falantes (boolean, padrão true)

**Response (200 OK)**
```json
{
  "correlationId": "def456",
  "model": "large-v3",
  "language": "pt",
  "numSpeakers": 2,
  "transcriptId": "tx-123",
  "downloadUrl": "/api/transcricao-audio/tx-123",
  "transcript": "Transcrição completa do áudio...",
  "segments": [
    {
      "speaker": "SPEAKER_00",
      "startMs": 0,
      "endMs": 3500,
      "text": "Primeira frase do áudio"
    }
  ],
  "metrics": {
    "processingTimeMs": 25000
  }
}
```

#### GET /api/transcricao-audio/{transcriptId}
Faz download de transcrição processada.

#### POST /api/transcricao-audio/upload/async
Inicia transcrição assíncrona e retorna `jobId` imediatamente.

#### GET /api/transcricao-audio/progresso/{jobId}
Retorna status e percentual de progresso da transcrição (`QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, `TIMEOUT`).

#### GET /api/transcricao-audio/progresso/{jobId}/stream
Stream SSE de progresso em tempo real (frontend usa SSE com fallback para polling).

Campos principais de progresso:
- `progressPercent`
- `estimated`
- `elapsedMs`
- `processedSeconds`
- `totalSeconds`
- `completed` (presente quando finalizado com sucesso)

### 4. RAG e Knowledge Source

#### POST /api/rag/context
Adiciona contexto em markdown na base de conhecimento e indexa no RAG.

Payload esperado:
```json
{
  "title": "Regulamento 2026",
  "contentMarkdown": "# Regras\nConteudo em markdown...",
  "category": "MotoGP",
  "source": "manual",
  "fileName": "regulamento-2026.md"
}
```

Comportamento:
- Salva o markdown em `knowledge-source/`.
- Executa upsert por `source` e hash de conteúdo.
- Reindexa automaticamente no H2 em memória.

### 3. Síntese de Fala (TTS)

#### POST /api/tts
Sintetiza texto em áudio.

**Request (JSON)**
```json
{
  "text": "Olá, mundo!",
  "voice": "default",
  "language": "pt-BR",
  "format": "wav"
}
```

**Response (200 OK - Audio Binary)**
- Content-Type: `audio/wav` ou `audio/mpeg`
- Content-Disposition: `inline; filename="tts-output.wav"`

#### GET /api/tts/voices
Lista vozes disponíveis para TTS.

**Response (200 OK)**
```json
["default", "alto", "soprano", "tenor"]
```

---

## Tratamento de Erros

### Códigos de Erro Principais

#### IA Local
- `IA_LOCAL_TIMEOUT` (504): Timeout ao processar requisição
- `IA_LOCAL_UNAVAILABLE` (503): Serviço indisponível
- `IA_LOCAL_INTERNAL_ERROR` (500): Erro interno

#### Transcrição
- `TRANSCRIPTION_FILE_TOO_LARGE` (413): Arquivo excede 200MB
- `TRANSCRIPTION_TIMEOUT` (504): Transcrição ultrapassou 120s
- `TRANSCRIPTION_SERVICE_UNAVAILABLE` (503): WhisperX indisponível
- `TRANSCRIPTION_FILE_NOT_FOUND` (404): Arquivo não encontrado

#### TTS
- `TTS_TIMEOUT` (504): Síntese ultrapassou 60s
- `TTS_UNAVAILABLE` (503): Serviço indisponível

#### Validação
- `INVALID_REQUEST` (400): Request malformado ou inválido

### Headers HTTP

**Response Headers Úteis**
- `X-Correlation-Id`: ID único para rastreamento
- `Content-Disposition`: Para downloads de arquivos
- `Content-Type`: Tipo de conteúdo (application/json, audio/wav, etc)

---

## Resolução de Problemas

### Erro: "ERR_CONNECTION_REFUSED" no Frontend

**Causas:**
- Backend não iniciou ainda (race condition)
- Backend não está disponível na porta 8080
- CORS não configurado

**Solução:**
1. Aguarde script exibir "Backend pronto"
2. Verifique: `curl -i http://localhost:8080/actuator/health`
3. Verifique logs: `logs/backend.log`

### Erro: "IA_LOCAL_TIMEOUT" (504)

**Causas:**
- Timeout default era 30s, muito curto
- Ollama sobrecarregado ou lento
- Modelo grande demora para processar

**Solução:**
1. Timeout já aumentado para 120s em `application.yml`
2. Se persistir, verificar logs do Ollama: `docker logs ollama`

### Erro: "TRANSCRIPTION_FILE_TOO_LARGE" (413)

**Causas:**
- Arquivo é maior que 200MB
- Spring multipart config desatualizado

**Solução:**
1. Arquivo máximo é 200MB
2. Verifique `application.yml` tem `max-file-size: 200MB`

### Erro: "Unknown Error (status:0)"

**Causas:**
- Erro de rede/CORS
- Request abortado antes de completar

**Solução:**
1. Verificar console do navegador (DevTools - F12)
2. Ativar painel DEV DEBUG no frontend (botão em canto)
3. Verificar correlation ID nos logs do backend

### Ollama/WhisperX não iniciam

**Causas:**
- Docker não está rodando
- Porta ocupada (11434, 9000)
- Sem espaço em disco

**Solução:**
1. Verificar Docker: `docker ps`
2. Verificar portas: `netstat -an | grep :11434`
3. Limpar: `docker compose down -v`
4. Reiniciar: `./start.bat` ou `./start.sh`

### Painel DEV DEBUG no Frontend

Disponível em modo desenvolvimento para visualizar:
- Correlation ID
- Timestamp do erro
- Erro técnico e funcional completo
- Response JSON bruto

**Para ativar:**
1. Desenvolvedor deve usar Chrome DevTools (F12)
2. Botão "DEBUG" aparece no topo se não em produção
3. Exibe detalhes de último erro capturado

---

## Logs

### Backend
- Logs padrão: `logs/backend.log`
- Logs de erro: `logs/backend.err.log`
- Com correlation ID em cada linha para rastreamento

### Frontend
- Console do navegador (DevTools)
- No painel DEV DEBUG (modo desenvolvimento)
- Logs npm: `logs/frontend.log`

### Docker
```bash
# Ollama
docker logs ollama

# WhisperX
docker logs whisperx

# Ver em tempo real
docker logs -f ollama
```

---

## Desenvolvimento

### Adicionar Novo Endpoint

1. **Controller** (`src/main/java/.../controllers/`)
2. **Use Case** (`src/main/java/.../application/usecases/`)
3. **Port** (Interface) (`src/main/java/.../application/ports/`)
4. **Adapter** (`src/main/java/.../adapters/outbound/`)
5. **Testes** (`src/test/java/`)

### Modificar Configurações

Editar `src/main/resources/application.yml` (requer restart)

### Build para Produção

```bash
# Backend
mvn clean package -DskipTests

# Frontend
cd frontend
npm run build

# Artefatos em:
# Backend: target/api-ia-1.0.jar
# Frontend: frontend/dist/api-ia/browser/
```

---

## Referências

- [RFC 7807 - Problem Details](https://tools.ietf.org/html/rfc7807)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Angular Documentation](https://angular.io/docs)
- [Resilience4j](https://resilience4j.readme.io/)
- [Ollama](https://ollama.ai)
- [WhisperX](https://github.com/m-bain/whisperX)

---

**Versão**: 1.0  
**Última atualização**: 20 de maio de 2026  
**Autor**: API-IA Team
