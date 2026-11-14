package br.com.stormaid.service;

import br.com.stormaid.ai.RagAnalysisService;
import br.com.stormaid.dto.request.IncidentCreateRequest;
import br.com.stormaid.entity.Dependent;
import br.com.stormaid.entity.IncidentReport;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.ChannelType;
import br.com.stormaid.messaging.IncidentEventPublisher;
import br.com.stormaid.repository.DependentRepository;
import br.com.stormaid.repository.IncidentReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentReportServiceTest {

    @Mock
    private IncidentReportRepository incidentReportRepository;

    @Mock
    private DependentRepository dependentRepository;

    @Mock
    private IncidentEventPublisher eventPublisher;

    @Mock
    private RagAnalysisService ragAnalysisService;

    @Mock
    private User requester;

    @Mock
    private IncidentCreateRequest request;

    @InjectMocks
    private IncidentReportService incidentReportService;

    @BeforeEach
    void setUp() {
        lenient().when(requester.getId()).thenReturn(1L);
        lenient().when(request.channelType())
                .thenReturn(ChannelType.TEXT_INPUT);
        lenient().when(request.textDescription())
                .thenReturn("Alagamento na residência");
        lenient().when(request.dependentId())
                .thenReturn((Long) null);
    }

    @Test
    void shouldCreateIncidentWithNewIdempotencyKey() {
        when(incidentReportRepository
                .findByRequesterIdAndIdempotencyKey(1L, "teste-001"))
                .thenReturn(Optional.empty());

        when(incidentReportRepository.save(any(IncidentReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = incidentReportService.createIncident(
                requester,
                request,
                "teste-001"
        );

        ArgumentCaptor<IncidentReport> captor =
                ArgumentCaptor.forClass(IncidentReport.class);

        verify(incidentReportRepository).save(captor.capture());

        assertEquals(
                "teste-001",
                captor.getValue().getIdempotencyKey()
        );

        assertNotNull(response);

        verify(eventPublisher).publishIncidentCreated(
                any(IncidentReport.class)
        );
    }

    @Test
    void shouldReturnExistingIncidentWithoutPublishingAgain() {
        IncidentReport previous = createPreviousIncident(
                "Alagamento na residência"
        );

        when(incidentReportRepository
                .findByRequesterIdAndIdempotencyKey(1L, "teste-001"))
                .thenReturn(Optional.of(previous));

        var response = incidentReportService.createIncident(
                requester,
                request,
                "teste-001"
        );

        assertNotNull(response);

        verify(incidentReportRepository, never())
                .save(any(IncidentReport.class));

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(ragAnalysisService);
    }

    @Test
    void shouldRejectSameKeyWithDifferentPayload() {
        IncidentReport previous = createPreviousIncident(
                "Incêndio na residência"
        );

        when(incidentReportRepository
                .findByRequesterIdAndIdempotencyKey(1L, "teste-001"))
                .thenReturn(Optional.of(previous));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> incidentReportService.createIncident(
                        requester,
                        request,
                        "teste-001"
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(incidentReportRepository, never())
                .save(any(IncidentReport.class));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldGenerateInternalKeyWhenHeaderIsMissing() {
        when(incidentReportRepository.save(any(IncidentReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        incidentReportService.createIncident(
                requester,
                request,
                null
        );

        ArgumentCaptor<IncidentReport> captor =
                ArgumentCaptor.forClass(IncidentReport.class);

        verify(incidentReportRepository).save(captor.capture());

        assertNotNull(captor.getValue().getIdempotencyKey());

        assertTrue(
                captor.getValue()
                        .getIdempotencyKey()
                        .startsWith("auto-")
        );
    }

    @Test
    void shouldAllowIncidentForOwnDependent() {
        Dependent dependent = new Dependent();
        dependent.setId(10L);
        dependent.setGuardian(requester);

        when(request.dependentId()).thenReturn(10L);

        when(dependentRepository.findById(10L))
                .thenReturn(Optional.of(dependent));

        when(incidentReportRepository.save(any(IncidentReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        incidentReportService.createIncident(
                requester,
                request,
                null
        );

        verify(incidentReportRepository).save(
                argThat(incident -> incident.getDependent() == dependent)
        );

        verify(eventPublisher).publishIncidentCreated(
                any(IncidentReport.class)
        );
    }

    @Test
    void shouldRejectIncidentForAnotherUsersDependent() {
        User anotherGuardian = new User();
        anotherGuardian.setId(2L);

        Dependent dependent = new Dependent();
        dependent.setId(10L);
        dependent.setGuardian(anotherGuardian);

        when(request.dependentId()).thenReturn(10L);

        when(dependentRepository.findById(10L))
                .thenReturn(Optional.of(dependent));

        assertThrows(
                AccessDeniedException.class,
                () -> incidentReportService.createIncident(
                        requester,
                        request,
                        null
                )
        );

        verify(incidentReportRepository, never())
                .save(any(IncidentReport.class));

        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(ragAnalysisService);
    }

    private IncidentReport createPreviousIncident(String description) {
        IncidentReport previous = new IncidentReport();

        previous.setRequester(requester);
        previous.setIdempotencyKey("teste-001");
        previous.setRecipientType(request.recipientType());
        previous.setThirdPartyName(request.thirdPartyName());
        previous.setRequesterOnSite(request.requesterOnSite());
        previous.setChannelType(request.channelType());
        previous.setEmergencyCategory(request.emergencyCategory());
        previous.setTextDescription(description);
        previous.setLocation(request.location());

        return previous;
    }
}
