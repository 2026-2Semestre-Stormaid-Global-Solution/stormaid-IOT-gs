
package br.com.stormaid.messaging;

import br.com.stormaid.config.RabbitMQConfig;
import br.com.stormaid.dto.event.IncidentCreatedEvent;
import br.com.stormaid.entity.IncidentReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private IncidentEventPublisher incidentEventPublisher;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void shouldPublishOnlyAfterCommit() {
        IncidentReport incident = createIncident();

        incidentEventPublisher.publishIncidentCreated(incident);

        verifyNoInteractions(rabbitTemplate);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.INCIDENT_CREATED_QUEUE),
                any(IncidentCreatedEvent.class)
        );
    }

    @Test
    void shouldNotPublishAfterRollback() {
        IncidentReport incident = createIncident();

        incidentEventPublisher.publishIncidentCreated(incident);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization ->
                        synchronization.afterCompletion(
                                TransactionSynchronization.STATUS_ROLLED_BACK
                        )
                );

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void shouldRejectPublicationWithoutTransaction() {
        TransactionSynchronizationManager.clear();

        IncidentReport incident = createIncident();

        assertThrows(
                IllegalStateException.class,
                () -> incidentEventPublisher.publishIncidentCreated(incident)
        );

        verifyNoInteractions(rabbitTemplate);
    }

    private IncidentReport createIncident() {
        IncidentReport incident = new IncidentReport();
        incident.setId(3L);
        return incident;
    }
}
