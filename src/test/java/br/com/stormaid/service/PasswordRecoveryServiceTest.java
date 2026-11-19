package br.com.stormaid.service;

import br.com.stormaid.dto.request.PasswordRecoveryRequest;
import br.com.stormaid.dto.request.PasswordRecoveryResetRequest;
import br.com.stormaid.dto.request.PasswordRecoveryVerifyRequest;
import br.com.stormaid.dto.response.PasswordRecoveryVerifyResponse;
import br.com.stormaid.entity.PasswordResetCode;
import br.com.stormaid.entity.User;
import br.com.stormaid.repository.PasswordResetCodeRepository;
import br.com.stormaid.repository.UserRepository;
import br.com.stormaid.security.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetCodeRepository resetCodeRepository;

    @Mock
    private PasswordResetAttemptService attemptService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private PasswordRecoveryService passwordRecoveryService;

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("senhaAntigaHash");
        return user;
    }

    @Test
    void shouldSendRecoveryCodeWhenUserExistsAndInvalidatePreviousCode() {
        String email = "cidadao@stormaid.example";
        User user = buildUser(1L, email);

        PasswordResetCode previousCode = PasswordResetCode.builder()
                .id(5L)
                .user(user)
                .codeHash("oldHash")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(resetCodeRepository.findByUserAndUsedFalse(user)).thenReturn(Optional.of(previousCode));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedCodeHash");
        when(resetCodeRepository.save(any(PasswordResetCode.class))).thenAnswer(inv -> inv.getArgument(0));

        PasswordRecoveryRequest request = new PasswordRecoveryRequest();
        request.setEmail("  CIDADAO@STORMAID.EXAMPLE  ");

        passwordRecoveryService.requestRecovery(request);

        assertTrue(previousCode.isUsed());
        verify(resetCodeRepository, times(2)).save(any(PasswordResetCode.class));
        verify(emailService).sendRecoveryCodeEmail(eq(email), anyString());
    }

    @Test
    void shouldThrowIllegalStateWhenRateLimitExceeded() {
        String email = "limite@stormaid.example";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        PasswordRecoveryRequest request = new PasswordRecoveryRequest();
        request.setEmail(email);

        for (int i = 0; i < 3; i++) {
            passwordRecoveryService.requestRecovery(request);
        }

        assertThrows(IllegalStateException.class, () -> passwordRecoveryService.requestRecovery(request));
        verify(emailService, never()).sendRecoveryCodeEmail(anyString(), anyString());
    }

    @Test
    void shouldVerifyValidCodeAndReturnTemporaryToken() {
        String email = "cidadao@stormaid.example";
        User user = buildUser(1L, email);

        PasswordResetCode activeCode = PasswordResetCode.builder()
                .id(10L)
                .user(user)
                .codeHash("hashed123456")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(resetCodeRepository.findByUserAndUsedFalse(user)).thenReturn(Optional.of(activeCode));
        when(passwordEncoder.matches("123456", "hashed123456")).thenReturn(true);
        when(tokenService.generatePasswordResetToken(user)).thenReturn("temp-jwt-token");

        PasswordRecoveryVerifyRequest verifyRequest = new PasswordRecoveryVerifyRequest();
        verifyRequest.setEmail(email);
        verifyRequest.setCode("123456");

        PasswordRecoveryVerifyResponse response = passwordRecoveryService.verifyCode(verifyRequest);

        assertEquals("temp-jwt-token", response.getTempToken());
        assertTrue(activeCode.isUsed());
        verify(resetCodeRepository).save(activeCode);
    }

    @Test
    void shouldRegisterFailedAttemptAndThrowIllegalArgumentOnWrongCode() {
        String email = "cidadao@stormaid.example";
        User user = buildUser(1L, email);

        PasswordResetCode activeCode = PasswordResetCode.builder()
                .id(10L)
                .user(user)
                .codeHash("hashed123456")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(resetCodeRepository.findByUserAndUsedFalse(user)).thenReturn(Optional.of(activeCode));
        when(passwordEncoder.matches("000000", "hashed123456")).thenReturn(false);
        when(attemptService.registerFailedAttempt(10L)).thenReturn(1);

        PasswordRecoveryVerifyRequest verifyRequest = new PasswordRecoveryVerifyRequest();
        verifyRequest.setEmail(email);
        verifyRequest.setCode("000000");

        assertThrows(IllegalArgumentException.class, () -> passwordRecoveryService.verifyCode(verifyRequest));
        verify(attemptService).registerFailedAttempt(10L);
    }

    @Test
    void shouldResetPasswordAndIncrementTokenVersion() {
        String email = "cidadao@stormaid.example";
        User user = buildUser(1L, email);
        long initialVersion = user.getTokenVersion();

        when(tokenService.validatePasswordResetToken("valid-temp-token")).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(tokenService.consumePasswordResetToken("valid-temp-token")).thenReturn(true);
        when(passwordEncoder.encode("NovaSenha!2026")).thenReturn("newEncodedPassword");
        when(resetCodeRepository.findByUserAndUsedFalse(user)).thenReturn(Optional.empty());

        PasswordRecoveryResetRequest resetRequest = new PasswordRecoveryResetRequest();
        resetRequest.setToken("valid-temp-token");
        resetRequest.setNewPassword("NovaSenha!2026");

        passwordRecoveryService.resetPassword(resetRequest);

        assertEquals("newEncodedPassword", user.getPassword());
        assertEquals(initialVersion + 1L, user.getTokenVersion());
        verify(userRepository).save(user);
    }
}