package br.com.stormaid.messaging.consumer;

import br.com.stormaid.ai.RagAnalysisService;
import br.com.stormaid.entity.IncidentReport;
import br.com.stormaid.enums.AiAnalysisStatus;
import br.com.stormaid.repository.IncidentAiAnalysisRepository;
import br.com.stormaid.repository.IncidentReportRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class IncidentAiConsumer {

    private static final Logger log = LoggerFactory.getLogger(IncidentAiConsumer.class);
    private final RagAnalysisService ragAnalysisService;
    private final IncidentReportRepository incidentRepository;
    private final IncidentAiAnalysisRepository aiAnalysisRepository;
    private final ObjectMapper objectMapper;

    public IncidentAiConsumer(RagAnalysisService ragAnalysisService,
                              IncidentReportRepository incidentRepository,
                              IncidentAiAnalysisRepository aiAnalysisRepository,
                              ObjectMapper objectMapper) {
        this.ragAnalysisService = ragAnalysisService;
        this.incidentRepository = incidentRepository;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "incident.ai.queue")
    public void consumeIncidentCreated(String messageJson) {
        try {
            IncidentEventPayload payload = objectMapper.readValue(messageJson, IncidentEventPayload.class);
            if (payload.getIncidentId() != null) {
                Long incidentId = payload.getIncidentId();
                log.info("Recebido evento de IA para o incidente ID: {}", incidentId);

                var existingAnalyses = aiAnalysisRepository.findByIncidentId(incidentId);
                boolean alreadyCompleted = existingAnalyses.stream()
                        .anyMatch(a -> a.getStatus() == AiAnalysisStatus.COMPLETED);

                if (alreadyCompleted) {
                    log.info("O incidente ID: {} já possui uma análise de IA concluída. Ignorando mensagem duplicada.", incidentId);
                    return;
                }

                IncidentReport incident = incidentRepository.findById(incidentId).orElse(null);
                if (incident != null) {
                    ragAnalysisService.analyzeIncident(incident);
                } else {
                    log.warn("Incidente ID: {} não encontrado na base de dados.", incidentId);
                }
            }
        } catch (Exception e) {
            log.error("Falha ao processar evento de IA para incidente: {}", e.getMessage());
            throw new RuntimeException("Falha no processamento da mensagem de IA", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IncidentEventPayload {
        private Long incidentId;
        public Long getIncidentId() { return incidentId; }
        public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }
    }
}