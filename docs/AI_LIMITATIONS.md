# Limitações da Inteligência Artificial - StormAid

Para garantir a segurança e a fiabilidade do sistema de emergência, o módulo de IA possui as seguintes restrições inegociáveis:

- **Sem Decisões Operacionais:** A IA não aprova nem rejeita solicitações, não decide gravidade clínica e não atribui viaturas, equipes ou rotas de socorro.
- **Substituição Humana Proibida:** O modelo atua exclusivamente como apoio à leitura; a decisão final e o despacho pertencem inteiramente ao Regulador humano.
- **Gestão de Dados Ausentes:** A IA está proibida de inventar dados de identificação, endereços, números de vítimas ou diagnósticos. Se a informação não constar no relato, é marcada como `null` ou ausente.
- **Abstenção por Falta de Fontes (`NO_SOURCES`):** Caso o corpus documental não contenha suporte suficiente para o relato, o sistema devolve o estado `NO_SOURCES` em vez de fabricar respostas ou alucinar URLs.
- **Resiliência a Falhas:** Se o provedor de LLM sofrer timeouts ou indisponibilidade, o fluxo operacional do chamado não é bloqueado; a central humana continua a operar normalmente com o relato original.