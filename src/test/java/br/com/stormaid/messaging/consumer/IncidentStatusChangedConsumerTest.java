package br.com.stormaid.messaging.consumer;

import br.com.stormaid.entity.IncidentReport;
import br.com.stormaid.enums.IncidentPublicStatus;
import br.com.stormaid.repository.IncidentReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentStatusChangedConsumerTest {

    @Mock
    private IncidentReportRepository incidentRepository;

    private IncidentStatusChangedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new IncidentStatusChangedConsumer(
                incidentRepository,
                new ObjectMapper()
        );
    }

    // 01 - Atualizar a ocorrência quando a versão recebida for mais recente
    @Test
    void shouldUpdateIncidentWhenEventVersionIsNewer() {
        IncidentReport incident = createIncident(1L);
        when(incidentRepository.findById(307L)).thenReturn(Optional.of(incident));

        consumer.consumeStatusChanged(message(
                "{\"eventType\":\"incident.status.changed.v1\"," +
                        "\"eventId\":\"evt-regulation-307-2\"," +
                        "\"incidentId\":307,\"status\":\"UNDER_REVIEW\"," +
                        "\"version\":2,\"changedAt\":\"2026-11-15T18:35:25Z\"}"
        ));

        assertEquals(IncidentPublicStatus.UNDER_REVIEW, incident.getPublicStatus());
        assertEquals(2L, incident.getStatusVersion());
        assertNotNull(incident.getUpdatedAt());
        verify(incidentRepository, times(1)).save(incident);
    }

    // 02 - Ignorar a repetição de um evento já aplicado
    @Test
    void shouldIgnoreDuplicatedEvent() {
        IncidentReport incident = createIncident(2L);
        when(incidentRepository.findById(307L)).thenReturn(Optional.of(incident));

        consumer.consumeStatusChanged(message(validEvent("UNDER_REVIEW", 2L)));

        assertEquals(IncidentPublicStatus.RECEIVED, incident.getPublicStatus());
        assertEquals(2L, incident.getStatusVersion());
        verify(incidentRepository, never()).save(any(IncidentReport.class));
    }

    // 03 - Ignorar eventos com uma versão anterior à versão atual
    @Test
    void shouldIgnoreOlderEvent() {
        IncidentReport incident = createIncident(3L);
        when(incidentRepository.findById(307L)).thenReturn(Optional.of(incident));

        consumer.consumeStatusChanged(message(validEvent("UNDER_REVIEW", 2L)));

        assertEquals(IncidentPublicStatus.RECEIVED, incident.getPublicStatus());
        assertEquals(3L, incident.getStatusVersion());
        verify(incidentRepository, never()).save(any(IncidentReport.class));
    }

    // 04 - Rejeitar JSON malformado sem consultar o repositório
    @Test
    void shouldRejectMalformedJson() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.consumeStatusChanged(message("{invalid-json")));

        verifyNoInteractions(incidentRepository);
    }

    // 05 - Propagar a falha caso a ocorrência não exista
    @Test
    void shouldRejectEventWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(307L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> consumer.consumeStatusChanged(
                        message(validEvent("UNDER_REVIEW", 2L))
                ));

        verify(incidentRepository, never()).save(any(IncidentReport.class));
    }

    // 06 - Rejeitar status inexistente sem persistir alterações
    @Test
    void shouldRejectUnknownStatus() {
        IncidentReport incident = createIncident(1L);
        when(incidentRepository.findById(307L)).thenReturn(Optional.of(incident));

        assertThrows(IllegalArgumentException.class,
                () -> consumer.consumeStatusChanged(
                        message(validEvent("UNKNOWN_STATUS", 2L))
                ));

        assertEquals(IncidentPublicStatus.RECEIVED, incident.getPublicStatus());
        assertEquals(1L, incident.getStatusVersion());
        verify(incidentRepository, never()).save(any(IncidentReport.class));
    }

    // 07 - Rejeitar evento sem os campos obrigatórios
    @Test
    void shouldRejectIncompleteEvent() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.consumeStatusChanged(
                        message("{\"incidentId\":307,\"status\":\"\",\"version\":2}")
                ));

        verifyNoInteractions(incidentRepository);
    }

    private IncidentReport createIncident(Long statusVersion) {
        IncidentReport incident = new IncidentReport();
        incident.setId(307L);
        incident.setPublicStatus(IncidentPublicStatus.RECEIVED);
        incident.setStatusVersion(statusVersion);
        return incident;
    }

    private String validEvent(String status, Long version) {
        return "{\"incidentId\":307,\"status\":\"" + status +
                "\",\"version\":" + version + "}";
    }

    private Message message(String json) {
        return new Message(
                json.getBytes(StandardCharsets.UTF_8),
                new MessageProperties()
        );
    }
}
