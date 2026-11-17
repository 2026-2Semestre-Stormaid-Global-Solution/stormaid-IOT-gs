# Avaliação e Testes de IA - StormAid

## Metodologia de Teste
Os testes foram conduzidos utilizando o conjunto de entradas sintéticas registado em `tests/ai/test-cases.csv`, avaliando a precisão da recuperação documental e a correta estruturação do JSON de saída.

## Critérios Avaliados
1. **Fidelidade Factual:** Verificação de que o resumo gerado contém apenas factos expressamente relatados pelo cidadão, sem adições ou invenções.
2. **Validade das Referências:** Confirmação de que todas as referências citadas no campo `sourceReferencesJson` correspondem exatamente a chunks recuperados do corpus oficial.
3. **Comportamento de Abstenção:** Testes com entradas fora do escopo (ex.: pedidos não relacionados com desastres) para garantir o retorno correto do estado `NO_SOURCES`.

## Resultados Observados
- O sistema demonstrou capacidade de extração estruturada bem-sucedida para cenários de inundações cobertos pelo corpus.
- Em cenários sem suporte documental correspondente, o modelo absteve-se corretamente de gerar fontes falsas, validando a robustez do mecanismo de segurança.