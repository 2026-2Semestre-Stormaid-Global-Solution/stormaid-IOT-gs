package br.com.stormaid.controller;

import br.com.stormaid.dto.request.IncidentCreateRequest;
import br.com.stormaid.dto.response.IncidentResponse;
import br.com.stormaid.entity.User;
import br.com.stormaid.service.IncidentReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentReportControllerRecoveryTest {

    @Mock
    private IncidentReportService incidentReportService;

    @Mock
    private User requester;

    @Mock
    private IncidentCreateRequest request;

    @Mock
    private IncidentResponse incidentResponse;

    @InjectMocks
    private IncidentReportController incidentReportController;

    @Test
    void shouldReturnCreatedWhenIncidentIsCreatedNormally() {
        when(incidentReportService.createIncident(
                requester,
                request,
                "teste-001"
        )).thenReturn(incidentResponse);

        ResponseEntity<IncidentResponse> response =
                incidentReportController.create(
                        requester,
                        "teste-001",
                        request
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(incidentResponse, response.getBody());

        verify(incidentReportService, never())
                .recoverIdempotencyConflict(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void shouldRecoverDataIntegrityViolationAndReturnExistingIncident() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Unique constraint violation"
                );

        when(incidentReportService.createIncident(
                requester,
                request,
                "teste-001"
        )).thenThrow(exception);

        when(incidentReportService.recoverIdempotencyConflict(
                requester,
                request,
                "teste-001",
                exception
        )).thenReturn(incidentResponse);

        ResponseEntity<IncidentResponse> response =
                incidentReportController.create(
                        requester,
                        "teste-001",
                        request
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(incidentResponse, response.getBody());

        verify(incidentReportService).recoverIdempotencyConflict(
                requester,
                request,
                "teste-001",
                exception
        );
    }

    @Test
    void shouldPropagateConflictWhenRecoveryDetectsDifferentPayload() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Unique constraint violation"
                );

        ResponseStatusException conflict =
                new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency-Key já utilizada com dados diferentes"
                );

        when(incidentReportService.createIncident(
                requester,
                request,
                "teste-001"
        )).thenThrow(exception);

        when(incidentReportService.recoverIdempotencyConflict(
                requester,
                request,
                "teste-001",
                exception
        )).thenThrow(conflict);

        ResponseStatusException result = assertThrows(
                ResponseStatusException.class,
                () -> incidentReportController.create(
                        requester,
                        "teste-001",
                        request
                )
        );

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertSame(conflict, result);
    }
}
