# 🚨 StormAid — IoT & IA (Assistente de Contextualização de Ocorrências)

O **StormAid** é uma plataforma inteligente de gestão de emergências, zeladoria comunitária e resposta a catástrofes. Nesta frente (**IoT & IA**), a solução integra canais de captura (`TEXT_INPUT`, `VOICE_AUDIO` e `SOS_SILENT` a partir do Mobile Citizen em React Native), processamento assíncrono via **RabbitMQ** e o **Assistente de Contextualização de Ocorrências** com **Spring AI + RAG** integrado ao backend **Java / Spring Boot** e à operação em **.NET**.

---

## 🚀 Swagger UI & Consoles

Acesse a documentação interativa e realize testes diretamente pelo navegador:

### Produção / Deploy
```bash
https://stormaid-java-gs.onrender.com/swagger-ui/index.html
```
---

🔗 Link YouTube (Pitch)
```bash
https://www.youtube.com/watch?v=ae4XAPM-IjA
```
---

## 👥 Integrantes do Grupo

<table>
  <tr>
    <td width="130">
      <img src="https://github.com/moisesBarsoti.png" width="120" style="border-radius: 50%;"/>
    </td>
    <td>
      <b>Moisés Barsoti Andrade de Oliveira</b><br/>
      <b>RM:</b> 565049 &nbsp;&nbsp;|&nbsp;&nbsp;<b>Turma:</b> 2TDSPO - FIAP <br/>
    </td>
  </tr>

  <tr>
    <td width="130">
      <img src="https://github.com/sSofia-s.png" width="120" style="border-radius: 50%;"/>
    </td>
    <td>
      <b>Sofia Siqueira Fontes</b><br/>
      <b>RM:</b> 563829 &nbsp;&nbsp;|&nbsp;&nbsp;<b>Turma:</b> 2TDSPG - FIAP <br/>
    </td>
  </tr>

  <tr>
    <td width="130">
      <img src="https://github.com/manuelalacerda.png" width="120" style="border-radius: 50%;"/>
    </td>
    <td>
      <b>Manuela de Lacerda Soares</b><br/>
      <b>RM:</b> 564887 &nbsp;&nbsp;|&nbsp;&nbsp;<b>Turma:</b> 2TDSPG - FIAP <br/>
    </td>
  </tr>
</table>

---

## 🎯 Objetivo da IA, Justificativas Técnicas e Responsabilidades

A funcionalidade de IA atua como **Assistente de Contextualização de Ocorrências**: transforma relatos de emergência em resumos objetivos e estruturados para apoiar a leitura do `REGULATOR`, recuperando trechos de uma base documental confiável via **Retrieval-Augmented Generation (RAG)**.

### Justificativas Técnicas das Escolhas Arquiteturais
- **IA como módulo no backend Java / Spring Boot (em vez de um terceiro backend separado)**: reutiliza o modelo de domínio já consolidado (`IncidentReport` e `IncidentAiAnalysis`), o controle transacional com Oracle/Flyway e as políticas de segurança/autorização existentes, evitando duplicação de entidades e reduzindo complexidade operacional.
- **Processamento assíncrono via RabbitMQ (`stormaid.events`)**: desacopla a criação da ocorrência (`201 Created`) da execução do RAG/STT, garantindo que o chamado seja persistido primeiro e que o atendimento pela central não seja bloqueado por latência, limite de quota ou indisponibilidade externa.
- **RAG restrito a fatos relatados e corpus documental curado**: fundamenta observações gerais em documentos oficiais de defesa civil/desastres e separa estritamente os fatos narrados pelo cidadão do contexto documental, reduzindo invenções e impedindo que o LLM tome decisões operacionais ou clínicas.

### O que a IA faz vs. O que a IA NÃO faz

- **Faz**: interpreta chamados por texto (`TEXT_INPUT`) e, após transcrição (STT), por voz (`VOICE_AUDIO`); identifica apenas fatos expressamente relatados (ex.: *"água entrando na residência"*, *"há pessoas ilhadas"*); explicita informações ausentes ou incertas sem inventá-las; recupera trechos de documentos selecionados sobre enchentes, alagamentos, deslizamentos e desastres climáticos e gera resumo estruturado com referências.
- **Não faz**: aprova ou rejeita solicitações; define gravidade clínica automaticamente; atribui viaturas, equipes, rotas ou prioridades operacionais; substitui o `REGULATOR`; promete atendimento; cria alertas meteorológicos em tempo real; nem inventa identificação, endereço, diagnóstico ou número de vítimas. O uso de RAG não elimina totalmente alucinações e exige validação adicional.

### Papéis no Ecossistema

- **Mobile Citizen (React Native)**: coleta texto, grava áudio com permissão explícita para microfone, envia ocorrências ao Java e exibe confirmação de registro (não executa embeddings, não define prioridade nem cria análises no backend).
- **Java / Spring Boot**: valida e persiste `IncidentReport`, executa STT (quando aplicável), roda **Spring AI + RAG**, grava `IncidentAiAnalysis` (`TB_STORMAID_INCIDENT_AI_ANALYSIS`) e publica eventos no broker (não cria `IncidentDispatch`, `RegulationDecision` nem seleciona viatura).
- **Base de Conhecimento RAG**: armazena conteúdos documentais indexados e metadados de proveniência (`documentId`, `chunkId`, `sourceUrl`, `section`) — não contém prontuários nem cadastros pessoais como corpus público.
- **RabbitMQ (`stormaid.events`)**: entrega eventos de ocorrência e disponibilização de análise a filas distintas com controle de idempotência (`eventId`), sem substituir o banco de dados nem transportar dados sensíveis desnecessários.
- **.NET / Regulator**: recebe projeção do chamado, consulta análise no Java por canal seguro e conduz a decisão de regulação/despacho (não recalcula RAG, não armazena senha nem assume propriedade de `IncidentAiAnalysis`).
- **Mobile Responder**: exibe despachos da `.NET` e dados essenciais autorizados, incluindo resumo quando necessário, sem consultas globais à base de relatos nem acesso a dados de outros atendimentos.

---

## 📊 Diagrama da Arquitetura (IoT / Canais → Java + RAG → RabbitMQ → .NET)

```mermaid
flowchart LR
    MC["Mobile Citizen<br/>(React Native: Texto, Voz, SOS)"] -->|"HTTP POST"| JAVA["API Java / Spring Boot<br/>(IncidentReport + Spring AI + RAG)"]
    JAVA -->|"Persiste análise & histórico (1:N)"| DB[("Oracle Database<br/>(Flyway)")]
    KB["Base de Conhecimento RAG<br/>(Documentos Curados + Metadados)"] -.->|"Recuperação (topK)"| JAVA
    JAVA -->|"incident.created.v1<br/>incident.ai.completed.v1"| MQ{"RabbitMQ<br/>stormaid.events"}
    MQ -->|"stormaid.java.ai-processor"| JAVA
    MQ -->|"stormaid.dotnet.incident-created<br/>stormaid.dotnet.ai-completed"| DOTNET[".NET / Regulator<br/>(Regulação & Despacho)"]
    DOTNET -->|"GET /api/v1/internal/incidents/{id}/ai-analysis"| JAVA
    DOTNET -->|"incident.status.changed.v1"| MQ
    DOTNET -->|"Despachos & dados autorizados"| MR["Mobile Responder"]
```

---

## 📡 Canais de Entrada e Fluxos Assíncronos

- **`TEXT_INPUT`**:
  1. Cidadão envia `POST /api/v1/incidents` com `channelType=TEXT_INPUT`, `textDescription`, categoria e localização.
  2. Java valida, registra `IncidentReport` com `publicStatus=RECEIVED` e retorna `201 Created`.
  3. Após o *commit*, o evento `incident.created.v1` fica disponível para a `.NET` e para o consumidor de IA.
  4. O consumidor Java busca o chamado no próprio banco por `incidentId`, executa o RAG e persiste `IncidentAiAnalysis` (`COMPLETED`, `NO_SOURCES` ou `FAILED`).
  5. Quando concluída com material válido, o Java publica `incident.ai.completed.v1` e a `.NET` apresenta o resultado após consulta interna autorizada.
- **`VOICE_AUDIO`**:
  1. Mobile grava áudio com permissão explícita e envia `multipart/form-data` para `POST /api/v1/incidents/voice` com `metadata` (JSON) e `audio` (arquivo).
  2. Java valida tipo/tamanho, armazena a mídia em repositório privado/interno e persiste primeiro o chamado (`201 Created`, `publicStatus=RECEIVED`, `transcriptionStatus=PENDING`).
  3. Trabalhador assíncrono executa Speech-to-Text (STT), atualiza `audioTranscription` e `transcriptionStatus` (`COMPLETED` ou `FAILED`) e, se houver transcrição válida, executa o RAG sobre o texto.
  4. Em falha de STT (`transcriptionStatus=FAILED`), o chamado permanece recebido e visível na `.NET` com categoria e localização originais.
- **`SOS_SILENT`**: registra o chamado imediatamente com `channelType=SOS_SILENT`, categoria e localização, sem inventar descrição nem bloquear o atendimento; no MVP pode não gerar `IncidentAiAnalysis` quando não há texto.
- **Tempo de Paz (`CommunityRiskReport`)**: fluxo convencional de zeladoria urbana na API Java, sem RAG no MVP.

### Mensageria (Exchange `stormaid.events` — `topic`, durável)

| Routing Key | Produtor | Fila / Consumidor | Finalidade |
| :--- | :--- | :--- | :--- |
| `incident.created.v1` | Java | `stormaid.dotnet.incident-created` | Criar `IncidentReference` na `.NET` |
| `incident.created.v1` | Java | `stormaid.java.ai-processor` | Iniciar análise (ou transcrição) assíncrona |
| `incident.ai.completed.v1` | Java | `stormaid.dotnet.ai-completed` | Notificar análise consultável (deduplicada por `eventId` via `ProcessedIntegrationEvent`) |
| `incident.status.changed.v1` | `.NET` | `stormaid.java.incident-status` | Atualizar estado público no Java com validação de versão (`version > statusVersion`) |

---

## 🗄️ Modelo de Dados (Reutilização no Java / Oracle)

- **Fonte do relato (`IncidentReport` / `TB_STORMAID_INCIDENT_REPORT`)**: utiliza campos como `id`, `channelType`, `emergencyCategory`, `textDescription`, `audioTranscription`, `transcriptionStatus`, `audioStorageKey`, `publicStatus`, `recipientType`, `dependent` e localização já registrada (coordenadas/endereço não devem ser inventados ou substituídos pelo LLM).
- **Saída da IA (`IncidentAiAnalysis` / `TB_STORMAID_INCIDENT_AI_ANALYSIS`)**: relacionamento `1:N` a partir de `IncidentReport` para manter histórico auditável de reprocessamentos, com `id`, `incident`, `status` (`PENDING`, `PROCESSING`, `COMPLETED`, `NO_SOURCES`, `FAILED`), `summary`, `extractedInformationJson` (`CLOB`), `sourceReferencesJson` (`CLOB`), `modelName`, `errorCode`, `createdAt` e `completedAt`.

---

## ⚙️ Modelos, Tecnologias, Variáveis de Ambiente e Execução

### Modelos e Tecnologias
- **Backend & IA**: Java 17/21, Spring Boot, Spring AI (`ChatModel` / `ChatClient`, `EmbeddingModel`, `VectorStore` e, quando aplicável, transcrição STT), RAG sobre base documental curada.
- **Persistência, Mensageria e Canais**: Oracle Database com Flyway, RabbitMQ, integração com serviço `.NET` e canais do Mobile Citizen (React Native).

### Pré-requisitos

- **Java 17 / 21** e **Maven Wrapper (`./mvnw` / `.\mvnw.cmd`)**
- **Oracle Database** (com **Flyway**) e **RabbitMQ**
- Provedor compatível com **Spring AI** configurado via variáveis de ambiente (sem comitar chaves reais ou segredos)

### Variáveis de Ambiente (Exemplo — nunca comitar chaves reais)

```properties
# Base de Dados Oracle
ORACLE_URL=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
ORACLE_USER=seu_usuario_rm
ORACLE_PASSWORD=sua_senha_oracle

# Mensageria RabbitMQ
RABBITMQ_HOST=127.0.0.1
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=stormaid
RABBITMQ_PASSWORD=sua_senha_rabbitmq
RABBITMQ_VIRTUAL_HOST=/

# Servidor & Segurança JWT
PORT=8080
JWT_SECRET=sua_chave_jwt_super_segura_aqui

# Inteligência Artificial (Spring AI)
SPRING_AI_OPENAI_API_KEY=sua_chave_api_ia_aqui
API_KEY=sua_chave_api_ia_aqui

# E-mail (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seu_email@exemplo.com
MAIL_PASSWORD=sua_senha_ou_app_password
```

### Inicialização e Índice / Reindexação do Corpus RAG

1. Suba os serviços de infraestrutura (**Oracle** e **RabbitMQ**):

   ```bash
   docker-compose up -d
   ```

2. Mantenha os documentos curados e o manifesto em `src/main/resources/knowledge/` (ex.: `manifest.json`, `flood-guidance.md`, `landslide-guidance.md`) e reconstrua/indexe o `VectorStore` na inicialização ou pela rotina de ingestão configurada no projeto (sem misturar embeddings de modelos/dimensões incompatíveis).

3. Inicie a aplicação Java:

   ```bash
   ./mvnw spring-boot:run
   ```

   *(No Windows PowerShell: `.\mvnw.cmd spring-boot:run`)*

---

## 📌 Endpoints Principais do Fluxo

| Método | Endpoint | Finalidade |
| :--- | :--- | :--- |
| `POST` | `/api/v1/incidents` | Cria ocorrência (`TEXT_INPUT` ou `SOS_SILENT`) e publica `incident.created.v1` |
| `POST` | `/api/v1/incidents/voice` | Cria ocorrência de áudio via `multipart/form-data` (`metadata` + `audio`) |
| `GET` | `/api/v1/incidents/{id}` | Consulta ocorrência própria e estado operacional |
| `GET` | `/api/v1/incidents?page=0&size=10` | Lista histórico paginado do solicitante |
| `GET` | `/api/v1/internal/incidents/{id}/ai-analysis` | Consulta interna autenticada da análise (`IncidentAiAnalysisResponse`) por serviço autorizado |

---

## 📊 Evidências de Avaliação: Casos de Teste, Métricas, Resultados e Limitações

- **Execução de testes automatizados**:
  ```bash
  ./mvnw clean test
  ```
- **Casos de teste e critérios avaliados**: cenários funcionais e negativos (ex.: `tests/ai/test-cases.csv`), cobrindo fidelidade factual aos fatos relatados, cobertura de informações essenciais, explicitação de dados ausentes (`peopleAtRisk=null` / `missingInformation`), validade das referências recuperadas (`documentId` / `chunkId`), abstenção adequada (`NO_SOURCES`), resistência a *prompt injection*, isolamento de acesso (`403` / autorização de serviço), idempotência de eventos e comportamento em falhas (`FAILED` / `transcriptionStatus=FAILED` / `SOS_SILENT`).
- **Métricas e resultados reais**: acompanhar fidelidade factual, cobertura de fatos relatados, validade das referências, taxa de abstenção adequada (`NO_SOURCES`) e tempo de análise (`completedAt - createdAt`). Registre os resultados reais medidos em `docs/AI_EVALUATION.md` (sem preencher métricas antecipadamente).
- **Limitações e resiliência**:
  - **Falha/Quota da IA (`HTTP 429` / timeout / JSON inválido)**: persiste `FAILED` (ou `NO_SOURCES` quando não há trechos relevantes), aplica *fallback* controlado e **não** bloqueia o registo da ocorrência nem o atendimento pela central humana.
  - **Falha de conexão com RabbitMQ ou STT**: preserva o chamado registrado; `transcriptionStatus=FAILED` mantém o registro visível com categoria e localização originais.
  - **RAG e dados não confiáveis**: RAG não elimina totalmente alucinações nem substitui validação humana; alertas meteorológicos dinâmicos em tempo real (como avisos do INMET) não são gerados por base documental estática; conteúdos recuperados e relatos não são comandos confiáveis e a saída do LLM nunca altera `publicStatus`, `RegulationDecision` ou despachos.
  - **Canal de voz (`VOICE_AUDIO`)**: se o STT real ainda não estiver ativo no ambiente de demonstração, declare essa limitação explicitamente sem simular transcrição real.
  - **Privacidade**: nunca gravar chaves reais, tokens, áudios privados ou dados sensíveis em logs ou arquivos versionados no GitHub.

> **Formato de envio na plataforma**: enviar **APENAS** um arquivo `.txt` contendo RM, nome completo dos integrantes e o link do repositório no GitHub, mantendo no repositório o código-fonte, dependências, configurações de exemplo sem credenciais, este `README.md`, fontes, diagrama, casos de teste, resultados reais, métricas e limitações.

---

## 📚 Fontes Documentais de Referência (Corpus RAG)

- **Secretaria Nacional de Proteção e Defesa Civil** — *Saiba como agir*: `https://www.gov.br/mdr/pt-br/assuntos/protecao-e-defesa-civil/saiba-como-agir`
- **Cemaden** — *Movimento de massa*: `https://www.gov.br/cemaden/pt-br/paginas/ameacas-naturais/movimento-de-massa`
- **Defesa Civil do Paraná** — *Deslizamento*: `https://www.defesacivil.pr.gov.br/Pagina/Deslizamento`
