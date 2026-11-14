package br.com.stormaid.controller;

import br.com.stormaid.dto.request.IncidentCreateRequest;
import br.com.stormaid.dto.response.IncidentResponse;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.ChannelType;
import br.com.stormaid.enums.EmergencyCategory;
import br.com.stormaid.enums.IncidentPublicStatus;
import br.com.stormaid.enums.RecipientType;
import br.com.stormaid.enums.UserStatus;
import br.com.stormaid.repository.UserRepository;
import br.com.stormaid.security.JwtAuthenticationFilter;
import br.com.stormaid.security.SecurityConfig;
import br.com.stormaid.security.TokenService;
import br.com.stormaid.service.IncidentReportService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IncidentReportController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class IncidentReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncidentReportService incidentReportService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserRepository userRepository;

    private User requester;

    private static final String REQUEST_JSON = """
            {
              "recipientType": "SELF",
              "channelType": "TEXT_INPUT",
              "emergencyCategory": "CLIMATE_DISASTER",
              "textDescription": "Alagamento na residência",
              "location": {
                "source": "CURRENT_LOCATION",
                "latitude": -23.5505,
                "longitude": -46.6333,
                "addressText": "São Paulo - SP"
              }
            }
            """;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(1L);
        requester.setEmail("cidadao@teste.com");
        requester.setStatus(UserStatus.ACTIVE);
    }

    private void authenticateRequester() {
        when(tokenService.validateToken("token-teste"))
                .thenReturn("cidadao@teste.com");

        when(userRepository.findByEmail("cidadao@teste.com"))
                .thenReturn(Optional.of(requester));
    }

    private IncidentResponse response() {
        return new IncidentResponse(
                100L,
                1L,
                RecipientType.SELF,
                ChannelType.TEXT_INPUT,
                EmergencyCategory.CLIMATE_DISASTER,
                IncidentPublicStatus.RECEIVED,
                null,
                Instant.parse("2026-11-14T12:00:00Z")
        );
    }

    @Test
    void shouldCreateIncidentWithIdempotencyHeader() throws Exception {
        authenticateRequester();

        when(incidentReportService.createIncident(
                eq(requester),
                any(IncidentCreateRequest.class),
                eq("teste-http-001")
        )).thenReturn(response());

        mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer token-teste")
                        .header("Idempotency-Key", "teste-http-001")
                        .contentType("application/json")
                        .content(REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.requesterUserId").value(1));

        verify(incidentReportService).createIncident(
                eq(requester),
                any(IncidentCreateRequest.class),
                eq("teste-http-001")
        );
    }

    @Test
    void shouldRejectIdempotencyConflict() throws Exception {
        authenticateRequester();

        when(incidentReportService.createIncident(
                eq(requester),
                any(IncidentCreateRequest.class),
                eq("teste-http-001")
        )).thenThrow(new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Idempotency-Key já utilizada com dados diferentes"
        ));

        mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer token-teste")
                        .header("Idempotency-Key", "teste-http-001")
                        .contentType("application/json")
                        .content(REQUEST_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectUnauthenticatedIncidentCreation() throws Exception {
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType("application/json")
                        .content(REQUEST_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidIncidentRequest() throws Exception {
        authenticateRequester();

        mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer token-teste")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
