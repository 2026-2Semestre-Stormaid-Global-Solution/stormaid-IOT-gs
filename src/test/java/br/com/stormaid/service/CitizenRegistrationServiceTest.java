package br.com.stormaid.service;

import br.com.stormaid.dto.request.AddressRequestDto;
import br.com.stormaid.dto.request.EmergencyContactRequestDto;
import br.com.stormaid.dto.request.RegisterCompleteRequest;
import br.com.stormaid.dto.request.RegisterRequest;
import br.com.stormaid.dto.request.VulnerabilityRequestDto;
import br.com.stormaid.dto.response.LoginResponse;
import br.com.stormaid.dto.response.UserResponse;
import br.com.stormaid.entity.CitizenProfile;
import br.com.stormaid.entity.User;
import br.com.stormaid.repository.CitizenProfileRepository;
import br.com.stormaid.repository.UserRepository;
import br.com.stormaid.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitizenRegistrationServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private CitizenRegistrationService citizenRegistrationService;

    private RegisterCompleteRequest baseRequest;
    private User createdUser;
    private UserResponse createdUserResponse;

    @BeforeEach
    void setUp() {
        RegisterRequest account = new RegisterRequest();
        account.setFullName("Maria Silva");
        account.setEmail("maria@stormaid.example");
        account.setPhoneNumber("+5511999999999");
        account.setCpf("12345678901");
        account.setPassword("SenhaForte!2026");

        AddressRequestDto address = new AddressRequestDto();
        setFieldIfExists(address, "postalCode", "01310100");
        setFieldIfExists(address, "street", "Avenida Paulista");
        setFieldIfExists(address, "addressNumber", "1578");
        setFieldIfExists(address, "complement", "Apto 101");
        setFieldIfExists(address, "neighborhood", "Bela Vista");
        setFieldIfExists(address, "city", "Sao Paulo");
        setFieldIfExists(address, "state", "sp");
        setFieldIfExists(address, "referencePoint", "Proximo ao metro");

        VulnerabilityRequestDto vulnerability = new VulnerabilityRequestDto();
        setFieldIfExists(vulnerability, "mobilityStatus", "NORMAL");
        setFieldIfExists(vulnerability, "hasVisualImpairment", false);
        setFieldIfExists(vulnerability, "hasHearingImpairment", false);
        setFieldIfExists(vulnerability, "hasSpeechImpairment", false);
        setFieldIfExists(vulnerability, "hasCognitiveImpairment", false);
        setFieldIfExists(vulnerability, "dependsOnEquipment", false);
        setFieldIfExists(vulnerability, "equipmentDescription", null);
        setFieldIfExists(vulnerability, "equipmentInformationDeclined", false);

        baseRequest = new RegisterCompleteRequest();
        baseRequest.setAccount(account);
        baseRequest.setAddress(address);
        baseRequest.setVulnerability(vulnerability);
        baseRequest.setNoEmergencyContact(true);

        createdUser = new User();
        createdUser.setId(42L);
        createdUser.setEmail("maria@stormaid.example");

        createdUserResponse = mock(UserResponse.class);
        lenient().when(createdUserResponse.getId()).thenReturn(42L);
    }

    @Test
    void deveCadastrarCompletoSemContatoEmergenciaERetornarJwt() {
        when(authService.register(baseRequest.getAccount())).thenReturn(createdUserResponse);
        when(userRepository.findById(42L)).thenReturn(Optional.of(createdUser));
        when(tokenService.generateToken(createdUser)).thenReturn("jwt-sem-contato");

        LoginResponse response = citizenRegistrationService.registerComplete(baseRequest);

        assertNotNull(response);
        assertEquals("jwt-sem-contato", extractToken(response));
        verify(citizenProfileRepository, times(1)).saveAndFlush(any(CitizenProfile.class));
    }

    @Test
    void deveCadastrarCompletoComContatoEmergenciaERetornarJwt() {
        EmergencyContactRequestDto contact = new EmergencyContactRequestDto();
        setFieldIfExists(contact, "name", "  Joao Silva  ");
        setFieldIfExists(contact, "phoneNumber", "+5511988887777");
        setFieldIfExists(contact, "relationship", "Irmao");

        baseRequest.setNoEmergencyContact(false);
        baseRequest.setEmergencyContact(contact);

        when(authService.register(baseRequest.getAccount())).thenReturn(createdUserResponse);
        when(userRepository.findById(42L)).thenReturn(Optional.of(createdUser));
        when(tokenService.generateToken(createdUser)).thenReturn("jwt-com-contato");

        LoginResponse response = citizenRegistrationService.registerComplete(baseRequest);

        assertNotNull(response);
        assertEquals("jwt-com-contato", extractToken(response));
        verify(citizenProfileRepository, times(1)).saveAndFlush(any(CitizenProfile.class));
    }

    @Test
    void devePropagarExcecaoQuandoEmailDuplicado() {
        when(authService.register(baseRequest.getAccount()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> citizenRegistrationService.registerComplete(baseRequest));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(citizenProfileRepository, never()).saveAndFlush(any());
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void devePropagarExcecaoQuandoCpfDuplicado() {
        when(authService.register(baseRequest.getAccount()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "CPF already exists"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> citizenRegistrationService.registerComplete(baseRequest));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(citizenProfileRepository, never()).saveAndFlush(any());
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void deveLancarExcecaoQuandoDadosEnderecoInvalidosOuNulos() {
        AddressRequestDto invalidAddress = new AddressRequestDto();
        setFieldIfExists(invalidAddress, "state", null);
        baseRequest.setAddress(invalidAddress);

        when(authService.register(baseRequest.getAccount())).thenReturn(createdUserResponse);
        when(userRepository.findById(42L)).thenReturn(Optional.of(createdUser));

        assertThrows(RuntimeException.class,
                () -> citizenRegistrationService.registerComplete(baseRequest));
        verify(citizenProfileRepository, never()).saveAndFlush(any());
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void deveFalharAoPersistirPerfilSemGerarTokenEPermitirNovaTentativaAposFalha() {
        when(authService.register(baseRequest.getAccount())).thenReturn(createdUserResponse);
        when(userRepository.findById(42L)).thenReturn(Optional.of(createdUser));
        when(citizenProfileRepository.saveAndFlush(any(CitizenProfile.class)))
                .thenThrow(new RuntimeException("Falha transacional ao salvar perfil"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenService.generateToken(createdUser)).thenReturn("jwt-apos-retry");

        assertThrows(RuntimeException.class,
                () -> citizenRegistrationService.registerComplete(baseRequest));
        verify(tokenService, never()).generateToken(any());

        // Nova tentativa apos falha nao duplica usuario e conclui o cadastro
        LoginResponse retryResponse = citizenRegistrationService.registerComplete(baseRequest);

        assertNotNull(retryResponse);
        assertEquals("jwt-apos-retry", extractToken(retryResponse));
        verify(authService, times(2)).register(baseRequest.getAccount());
        verify(citizenProfileRepository, times(2)).saveAndFlush(any(CitizenProfile.class));
    }

    private static String extractToken(LoginResponse response) {
        if (response == null) {
            return null;
        }
        String[] candidateMethods = {"getToken", "token", "getAccessToken", "accessToken", "jwt"};
        for (String methodName : candidateMethods) {
            try {
                Method m = response.getClass().getMethod(methodName);
                Object value = m.invoke(response);
                if (value != null) {
                    return value.toString();
                }
            } catch (Exception ignored) {
            }
        }
        String[] candidateFields = {"token", "accessToken", "jwt"};
        for (String fieldName : candidateFields) {
            try {
                Field f = response.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                Object value = f.get(response);
                if (value != null) {
                    return value.toString();
                }
            } catch (Exception ignored) {
            }
        }
        fail("Nao foi possivel localizar o metodo ou campo de token em LoginResponse");
        return null;
    }

    private static void setFieldIfExists(Object target, String fieldName, Object value) {
        if (target == null) {
            return;
        }
        try {
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                    m.invoke(target, value);
                    return;
                }
            }
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception ignored) {
            // Ignora campos que tenham nomes diferentes no DTO local
        }
    }
}