package br.com.stormaid.controller;

import br.com.stormaid.dto.request.LoginRequest;
import br.com.stormaid.dto.request.RegisterCompleteRequest;
import br.com.stormaid.dto.response.LoginResponse;
import br.com.stormaid.dto.response.UserResponse;
import br.com.stormaid.repository.UserRepository;
import br.com.stormaid.security.TokenService;
import br.com.stormaid.service.AuthService;
import br.com.stormaid.service.CitizenRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private CitizenRegistrationService citizenRegistrationService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserRepository userRepository;

    private static final String VALID_REGISTER_COMPLETE_PAYLOAD = """
            {
              "noEmergencyContact": true,
              "account": {
                 "fullName": "Ana Silva",
                 "email": "ana@stormaid.example",
                 "phoneNumber": "+5511999999999",
                 "cpf": "52998224725",
                 "birthDate": "1998-05-15",
                 "password": "SenhaForte!2026"
               },
              "address": {
                "postalCode": "01310100",
                "street": "Avenida Paulista",
                "addressNumber": "1000",
                "neighborhood": "Bela Vista",
                "city": "Sao Paulo",
                "state": "SP"
              },
              "vulnerability": {
                "mobilityStatus": "AUTONOMOUS",
                "hasVisualImpairment": false,
                "hasHearingImpairment": false,
                "hasSpeechImpairment": false,
                "hasCognitiveImpairment": false,
                "dependsOnEquipment": false,
                "equipmentInformationDeclined": false
              }
            }
            """;

    @Test
    void deveRetornar201EJwtNoCadastroCompletoValido() throws Exception {
        UserResponse userInfo = mock(UserResponse.class);
        LoginResponse loginResponse = new LoginResponse("jwt-valido-201", "Bearer", 7200L, userInfo);

        when(citizenRegistrationService.registerComplete(any(RegisterCompleteRequest.class)))
                .thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/register-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_COMPLETE_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt-valido-201"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void deveRetornar409QuandoEmailOuCpfDuplicado() throws Exception {
        when(citizenRegistrationService.registerComplete(any(RegisterCompleteRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"));

        mockMvc.perform(post("/api/v1/auth/register-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_COMPLETE_PAYLOAD))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRetornar400QuandoRequisicaoCadastroInvalida() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar200NoLoginValido() throws Exception {
        UserResponse userInfo = mock(UserResponse.class);
        LoginResponse loginResponse = new LoginResponse("jwt-login", "Bearer", 7200L, userInfo);

        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        String loginPayload = """
                {
                  "email": "ana@stormaid.example",
                  "password": "SenhaForte!2026"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-login"));
    }
}