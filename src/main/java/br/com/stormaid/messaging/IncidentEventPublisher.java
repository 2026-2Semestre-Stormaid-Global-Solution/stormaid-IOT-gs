package br.com.stormaid.messaging;

import br.com.stormaid.config.RabbitMQConfig;
import br.com.stormaid.dto.event.IncidentCreatedEvent;
import br.com.stormaid.entity.IncidentReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class IncidentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(IncidentEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public IncidentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishIncidentCreated(IncidentReport incident) {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("A publicação de ocorrências exige uma transação ativa");
        }

        Long incidentId = incident.getId();

        IncidentCreatedEvent event = new IncidentCreatedEvent(
                incidentId,
                incident.getRequester() != null ? incident.getRequester().getId() : null,
                incident.getRecipientType(),
                incident.getChannelType(),
                incident.getEmergencyCategory(),
                incident.getPublicStatus(),
                incident.getLocation(),
                incident.getCreatedAt()
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        try {
                            rabbitTemplate.convertAndSend(
                                    RabbitMQConfig.INCIDENT_CREATED_QUEUE,
                                    event
                            );

                            logger.info(
                                    "Envio ao RabbitMQ solicitado após commit. incidentId={}",
                                    incidentId
                            );

                        } catch (Exception exception) {
                            logger.error(
                                    "Falha ao enviar ocorrência ao RabbitMQ após commit. incidentId={}",
                                    incidentId,
                                    exception
                            );
                        }
                    }
                }
        );
    }
}