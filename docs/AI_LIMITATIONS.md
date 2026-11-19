# Limitações da Inteligência Artificial - StormAID

Para garantir a segurança e a fiabilidade do sistema de emergência, o módulo de IA possui as seguintes restrições inegociáveis:

- **Orientação de Apoio vs. Avisos Oficiais:** A saída da IA é apenas apoio à leitura e triagem informativa; não substitui avisos oficiais da Defesa Civil nem comunicações institucionais.
- **Sem Decisões Operacionais:** A IA não aprova nem rejeita solicitações, não decide gravidade clínica e não atribui viaturas, equipes ou rotas de socorro.
- **Substituição Humana Proibida:** A decisão final e o despacho pertencem inteiramente ao operador/regulador humano.
- **Privacidade e Dados Ausentes:** Não são enviados dados pessoais desnecessários ao modelo, e informações não declaradas no relato não são inventadas.
- **Abstenção (`NO_SOURCES`) e Falhas (`FAILED`):** Sem suporte documental, o sistema retorna `NO_SOURCES`; em erro do modelo ou JSON inválido, grava `FAILED` (`AI_PROCESSING_ERROR`) e aciona o fluxo de *retry*/DLQ sem bloquear o registro da ocorrência.
- **Transcrição de Áudio Simulada:** O `SpeechTranscriptionService` produz uma transcrição simulada para fins de fluxo/teste e não realiza reconhecimento real de voz; o conteúdo simulado não deve ser tratado como transcrição real de áudio.