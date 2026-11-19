# Arquitetura de Inteligência Artificial e RAG - StormAID

## Visão Geral
A funcionalidade de IA do StormAID atua como um assistente de contextualização de ocorrências para apoiar a central de atendimento (Regulador). O fluxo baseia-se em **RAG (Retrieval-Augmented Generation)** utilizando o **Spring AI**.

## Componentes Principais
1. **Base de Conhecimento (`src/main/resources/knowledge/`):**
   - Contém documentos curados em Markdown (`guia-emergencias.md`) e o ficheiro `manifest.json` com metadados e proveniência oficial.
   - O `VectorStoreLoader` divide o conteúdo em *chunks* e anexa metadados (`documentId`, `title`, `sourceUrl` e `section`) antes de indexar no `VectorStore`.
2. **Pipeline de Recuperação e Geração (`RagAnalysisService`):**
   - Extrai apenas o conteúdo textual do relato (`textDescription` ou `audioTranscription`) e a `emergencyCategory`, sem enviar dados pessoais desnecessários do solicitante ao modelo.
   - Consulta o `VectorStore` por similaridade e, se não houver documentos relevantes ou texto, grava o estado `NO_SOURCES`.
   - Envia o contexto recuperado ao `ChatClient` com instrução para retornar JSON estrito (`summary`, `extractedInformation`, `sources`).
3. **Persistência, Observabilidade e Mensageria (RabbitMQ):**
   - O incidente é persistido previamente na base de dados e o evento de criação despoleta o processamento assíncrono via `IncidentAiConsumer`.
   - Cada execução regista entrada/contexto/saída na entidade `IncidentAiAnalysis` (`status`, `summary`, `extractedInformationJson`, `sourceReferencesJson`, `errorCode`, `completedAt`) e publica `incident.ai.completed.v1` quando concluída.
4. **Transcrição de Áudio (`SpeechTranscriptionService`):**
   - Executa de forma assíncrona (`@Async`) a atualização de `audioTranscription` e `transcriptionStatus` (`COMPLETED` ou `FAILED`).
   - **Estado atual:** Implementação **simulada** (gera texto marcador com `audioStorageKey`, sem integração com serviço real de *Speech-to-Text*).