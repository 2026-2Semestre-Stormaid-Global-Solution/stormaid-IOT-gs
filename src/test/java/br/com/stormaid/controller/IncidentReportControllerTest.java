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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentReportController.class)
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

    /**
     * Autenticação com a entidade User real como principal (necessário para
     * @AuthenticationPrincipal User) e a authority ROLE_CITIZEN exigida pelo
     * @PreAuthorize("hasRole('CITIZEN')") do controller.
     */
    private Authentication citizenAuth() {
        return new UsernamePasswordAuthenticationToken(
                requester,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))
        );
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
        when(incidentReportService.createIncident(
                any(User.class),
                any(IncidentCreateRequest.class),
                eq("teste-http-001")
        )).thenReturn(response());

        mockMvc.perform(post("/api/v1/incidents")
                        .with(authentication(citizenAuth()))
                        .header("Idempotency-Key", "teste-http-001")
                        .contentType("application/json")
                        .content(REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.requesterUserId").value(1));

        verify(incidentReportService).createIncident(
                any(User.class),
                any(IncidentCreateRequest.class),
                eq("teste-http-001")
        );
    }

    @Test
    void shouldRejectIdempotencyConflict() throws Exception {
        when(incidentReportService.createIncident(
                any(User.class),
                any(IncidentCreateRequest.class),
                eq("teste-http-001")
        )).thenThrow(new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Idempotency-Key já utilizada com dados diferentes"
        ));

        mockMvc.perform(post("/api/v1/incidents")
                        .with(authentication(citizenAuth()))
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
        mockMvc.perform(post("/api/v1/incidents")
                        .with(authentication(citizenAuth()))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}