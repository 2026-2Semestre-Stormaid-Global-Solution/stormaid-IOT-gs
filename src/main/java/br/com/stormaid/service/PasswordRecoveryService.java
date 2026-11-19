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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordRecoveryService {

    private static final int EXPIRATION_MINUTES = 15;
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 900L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository resetCodeRepository;
    private final PasswordResetAttemptService attemptService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    private final Map<String, Deque<Instant>> requestTimestampsByEmail = new ConcurrentHashMap<>();

    public PasswordRecoveryService(UserRepository userRepository,
                                   PasswordResetCodeRepository resetCodeRepository,
                                   PasswordResetAttemptService attemptService,
                                   EmailService emailService,
                                   PasswordEncoder passwordEncoder,
                                   TokenService tokenService) {
        this.userRepository = userRepository;
        this.resetCodeRepository = resetCodeRepository;
        this.attemptService = attemptService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public void requestRecovery(PasswordRecoveryRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (!allowRequest(normalizedEmail)) {
            throw new IllegalStateException("Muitas solicitações de recuperação. Tente novamente mais tarde.");
        }

        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        int codeInt = 100000 + SECURE_RANDOM.nextInt(900000);
        String codeStr = String.valueOf(codeInt);

        resetCodeRepository.findByUserAndUsedFalse(user).ifPresent(existing -> {
            existing.setUsed(true);
            resetCodeRepository.save(existing);
        });

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        PasswordResetCode resetCode = PasswordResetCode.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(codeStr))
                .expiresAt(nowUtc.plusMinutes(EXPIRATION_MINUTES))
                .used(false)
                .attempts(0)
                .build();

        resetCodeRepository.save(resetCode);
        emailService.sendRecoveryCodeEmail(user.getEmail(), codeStr);
    }

    private boolean allowRequest(String email) {
        Instant now = Instant.now();
        Instant threshold = now.minusSeconds(RATE_LIMIT_WINDOW_SECONDS);
        Deque<Instant> timestamps = requestTimestampsByEmail.computeIfAbsent(email, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS_PER_WINDOW) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    @Transactional
    public PasswordRecoveryVerifyResponse verifyCode(PasswordRecoveryVerifyRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Código inválido ou expirado"));

        PasswordResetCode resetCode = resetCodeRepository.findByUserAndUsedFalse(user)
                .orElseThrow(() -> new IllegalArgumentException("Nenhum pedido de recuperação ativo encontrado ou código bloqueado"));

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        if (resetCode.getExpiresAt().isBefore(nowUtc) || resetCode.getExpiresAt().isEqual(nowUtc)) {
            attemptService.markAsUsed(resetCode.getId());
            throw new IllegalStateException("O código de recuperação expirou");
        }

        if (resetCode.getAttempts() >= PasswordResetAttemptService.MAX_ATTEMPTS) {
            attemptService.markAsUsed(resetCode.getId());
            throw new IllegalStateException("Número máximo de tentativas excedido. Solicite um novo código.");
        }

        if (!passwordEncoder.matches(request.getCode(), resetCode.getCodeHash())) {
            int updatedAttempts = attemptService.registerFailedAttempt(resetCode.getId());
            if (updatedAttempts >= PasswordResetAttemptService.MAX_ATTEMPTS) {
                throw new IllegalStateException("Número máximo de tentativas excedido. Solicite um novo código.");
            }
            throw new IllegalArgumentException("Código incorreto");
        }

        resetCode.setUsed(true);
        resetCodeRepository.save(resetCode);

        String tempToken = tokenService.generatePasswordResetToken(user);

        return new PasswordRecoveryVerifyResponse(tempToken, "Código validado com sucesso");
    }

    @Transactional
    public void resetPassword(PasswordRecoveryResetRequest request) {
        String email = tokenService.validatePasswordResetToken(request.getToken());
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Token temporário inválido, expirado ou já utilizado");
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado"));

        boolean consumed = tokenService.consumePasswordResetToken(request.getToken());
        if (!consumed) {
            throw new IllegalArgumentException("Token temporário inválido, expirado ou já utilizado");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.incrementTokenVersion();
        userRepository.save(user);

        resetCodeRepository.findByUserAndUsedFalse(user).ifPresent(activeCode -> {
            activeCode.setUsed(true);
            resetCodeRepository.save(activeCode);
        });
    }
}