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

import java.util.HashMap;
import java.util.Map;

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
                        boolean sent = false;
                        int maxAttempts = 3;

                        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                            try {
                                rabbitTemplate.convertAndSend(
                                        RabbitMQConfig.INCIDENT_CREATED_QUEUE,
                                        event
                                );

                                logger.info(
                                        "Envio ao RabbitMQ realizado com sucesso após commit. incidentId={}, tentativa={}",
                                        incidentId,
                                        attempt
                                );
                                sent = true;
                                break;

                            } catch (Exception exception) {
                                logger.warn(
                                        "Tentativa {} de {} falhou ao enviar ocorrência ao RabbitMQ após commit. incidentId={}",
                                        attempt,
                                        maxAttempts,
                                        incidentId,
                                        exception
                                );

                                try {
                                    Thread.sleep(1000L * attempt);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                        }

                        if (!sent) {
                            logger.error(
                                    "ERRO CRÍTICO DE RECUPERAÇÃO: Falha definitiva ao enviar ocorrência ao RabbitMQ após {} tentativas. O evento incidentId={} requer reprocessamento.",
                                    maxAttempts,
                                    incidentId
                            );
                        }
                    }
                }
        );
    }

    public void publishIncidentCreatedForAi(IncidentReport incident) {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("A publicação do evento de IA exige uma transação ativa");
        }

        Long incidentId = incident.getId();
        Map<String, Object> aiPayload = new HashMap<>();
        aiPayload.put("incidentId", incidentId);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        boolean sent = false;
                        int maxAttempts = 3;

                        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                            try {
                                // Utiliza a exchange 'stormaid.events' e a routing key 'incident.created.ai'
                                rabbitTemplate.convertAndSend(
                                        "stormaid.events",
                                        "incident.created.ai",
                                        aiPayload
                                );

                                logger.info(
                                        "Envio do evento de IA ao RabbitMQ realizado com sucesso após commit. incidentId={}, tentativa={}",
                                        incidentId,
                                        attempt
                                );
                                sent = true;
                                break;

                            } catch (Exception exception) {
                                logger.warn(
                                        "Tentativa {} de {} falhou ao enviar evento de IA ao RabbitMQ após commit. incidentId={}",
                                        attempt,
                                        maxAttempts,
                                        incidentId,
                                        exception
                                );

                                try {
                                    Thread.sleep(1000L * attempt);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                        }

                        if (!sent) {
                            logger.error(
                                    "ERRO CRÍTICO: Falha definitiva ao enviar evento de IA ao RabbitMQ após {} tentativas. incidentId={}",
                                    maxAttempts,
                                    incidentId
                            );
                        }
                    }
                }
        );
    }
}