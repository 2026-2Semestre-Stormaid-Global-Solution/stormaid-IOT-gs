package br.com.stormaid.messaging.consumer;

import br.com.stormaid.entity.IncidentReport;
import br.com.stormaid.enums.IncidentPublicStatus;
import br.com.stormaid.repository.IncidentReportRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class IncidentStatusChangedConsumer {

    private static final Logger log = LoggerFactory.getLogger(IncidentStatusChangedConsumer.class);

    private final IncidentReportRepository incidentRepository;
    private final ObjectMapper objectMapper;

    public IncidentStatusChangedConsumer(
            IncidentReportRepository incidentRepository,
            ObjectMapper objectMapper
    ) {
        this.incidentRepository = incidentRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "incident.status.changed.queue")
    @Transactional
    public void consumeStatusChanged(String messageJson) {

        StatusChangedEvent event;

        try {
            event = objectMapper.readValue(
                    messageJson,
                    StatusChangedEvent.class
            );
        } catch (JsonProcessingException e) {
            log.error("Erro ao interpretar evento de atualização de status.", e);
            throw new IllegalArgumentException("Evento JSON inválido.", e);
        }

        if (event.getIncidentId() == null ||
                event.getIncidentId() <= 0 ||
                event.getStatus() == null ||
                event.getStatus().isBlank() ||
                event.getVersion() == null ||
                event.getVersion() <= 0) {

            throw new IllegalArgumentException(
                    "Evento de atualização de status incompleto ou inválido."
            );
        }

        IncidentReport incident = incidentRepository
                .findById(event.getIncidentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Ocorrência não encontrada: " + event.getIncidentId()
                ));

        if (event.getVersion() <= incident.getStatusVersion()) {
            log.info(
                    "Evento ignorado para a ocorrência {}. Versão recebida: {}, versão atual: {}.",
                    event.getIncidentId(),
                    event.getVersion(),
                    incident.getStatusVersion()
            );
            return;
        }

        IncidentPublicStatus newStatus = IncidentPublicStatus.valueOf(
                event.getStatus()
        );

        incident.setPublicStatus(newStatus);
        incident.setStatusVersion(event.getVersion());
        incident.setUpdatedAt(Instant.now());

        incidentRepository.save(incident);

        log.info(
                "Status da ocorrência {} atualizado para {}. Versão: {}.",
                incident.getId(),
                newStatus,
                event.getVersion()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusChangedEvent {

        private Long incidentId;
        private String status;
        private Long version;

        public Long getIncidentId() {
            return incidentId;
        }

        public void setIncidentId(Long incidentId) {
            this.incidentId = incidentId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }
    }
}