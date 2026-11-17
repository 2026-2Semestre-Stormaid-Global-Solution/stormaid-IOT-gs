# Arquitetura de Inteligência Artificial e RAG - StormAid

## Visão Geral
A funcionalidade de IA do StormAid atua como um assistente de contextualização de ocorrências para apoiar a central de atendimento (Regulador). O fluxo baseia-se em **RAG (Retrieval-Augmented Generation)** utilizando o **Spring AI**.

## Componentes Principais
1. **Base de Conhecimento (`src/main/resources/knowledge/`):**
    - Contém documentos curados em Markdown (como diretrizes de enchentes e deslizamentos) e o ficheiro `manifest.json` com metadados e proveniência oficial.
2. **Pipeline de Recuperação e Geração:**
    - O Spring AI processa os documentos, armazena-os num repositório vetorial (`VectorStore`) e efetua buscas de similaridade baseadas no relato do cidadão.
    - O modelo de linguagem (`ChatModel`) gera um resumo estruturado e fidedigno estritamente suportado pelos chunks recuperados.
3. **Persistência e Mensageria (RabbitMQ):**
    - O incidente é persistido previamente na base de dados Oracle (`TB_STORMAID_INCIDENT_REPORT`).
    - O evento de criação (`incident.created.v1`) despoleta o processamento assíncrono da IA.
    - Após a conclusão, a análise é guardada em `TB_STORMAID_INCIDENT_AI_ANALYSIS` e é publicado o evento `incident.ai.completed.v1` para o ecossistema .NET.