# Avaliação e Testes de IA - StormAID

## Metodologia de Teste
A validação do RAG considera os cenários operacionais do cartão **IOT-03**, verificando recuperação documental, estrutura JSON de saída, resiliência a falhas e ausência de envio de dados pessoais desnecessários.

## Cenários Avaliados
1. **Enchente (`CLIMATE_DISASTER`):** Relato de inundação/alagamento recuperando diretrizes de evacuação e desligamento de energia elétrica.
2. **Deslizamento:** Relato de encosta/rachaduras/inclinação recuperando sinais de risco estrutural e deslizamento.
3. **Mobilidade reduzida:** Relato envolvendo cadeirante, idoso ou pessoa com mobilidade reduzida, verificando destaque à evacuação prioritária sem inventar dados.
4. **Descrição incompleta / sem fontes:** Relato vazio, sem texto ou sem correspondência no `VectorStore`, retornando `NO_SOURCES`.
5. **Indisponibilidade do modelo:** Falha na chamada ao `ChatClient`, registrando `FAILED` (`AI_PROCESSING_ERROR`) e propagando erro para o ciclo de *retry*/DLQ do consumidor.
6. **Resposta inválida:** Retorno do LLM fora do formato JSON esperado, registrando `FAILED` (`AI_PROCESSING_ERROR`) e propagando erro para *retry*/DLQ.

## Critérios de Aceite
- **Fidelidade Factual:** Resumo restrito aos fatos narrados e trechos recuperados.
- **Privacidade:** Envio ao modelo apenas do texto do relato e categoria, sem dados pessoais sensíveis/desnecessários.
- **Diferenciação:** Orientação de apoio gerada por IA claramente separada de avisos oficiais e decisões da central humana.
- 
## Resultados Observados e Reprodutibilidade
- **Reprodutibilidade:** Subir a aplicação (`StormaidApplication`) com chave válida da OpenAI e RabbitMQ ativo para carregar o `guia-emergencias.md` no `VectorStore` via `VectorStoreLoader` e disparar ocorrências via API/Swagger.
- **Cenários de relato (enchente, deslizamento, mobilidade reduzida e descrição incompleta):** Validado o fluxo de recuperação por similaridade no `VectorStore`, retorno `NO_SOURCES` quando não há texto ou documentos correspondentes e geração de JSON estruturado (`summary`, `extractedInformation`, `sources`) restrito ao relato e aos trechos recuperados.
- **Cenários de falha (indisponibilidade do modelo e resposta inválida):** Validado que erros na chamada ao `ChatClient` ou JSON inválido registram `FAILED` (`AI_PROCESSING_ERROR`) e propagam exceção para o fluxo de *retry*/DLQ do consumidor.
- **Privacidade e separação de responsabilidades:** Confirmado que apenas o texto do relato e a categoria são enviados ao modelo (sem dados pessoais desnecessários) e que o resultado serve apenas como apoio ao operador humano, sem substituir avisos oficiais.