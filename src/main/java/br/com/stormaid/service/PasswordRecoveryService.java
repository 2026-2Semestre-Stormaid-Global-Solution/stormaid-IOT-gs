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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository resetCodeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public PasswordRecoveryService(UserRepository userRepository,
                                   PasswordResetCodeRepository resetCodeRepository,
                                   EmailService emailService,
                                   PasswordEncoder passwordEncoder,
                                   TokenService tokenService) {
        this.userRepository = userRepository;
        this.resetCodeRepository = resetCodeRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public void requestRecovery(PasswordRecoveryRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail().trim().toLowerCase());

        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        SecureRandom random = new SecureRandom();
        int codeInt = 100000 + random.nextInt(900000);
        String codeStr = String.valueOf(codeInt);

        resetCodeRepository.findByUserAndUsedFalse(user).ifPresent(existing -> {
            existing.setUsed(true);
            resetCodeRepository.save(existing);
        });

        PasswordResetCode resetCode = PasswordResetCode.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(codeStr))
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(15))
                .used(false)
                .build();

        resetCodeRepository.save(resetCode);
        emailService.sendRecoveryCodeEmail(user.getEmail(), codeStr);
    }

    @Transactional
    public PasswordRecoveryVerifyResponse verifyCode(PasswordRecoveryVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Código inválido ou expirado"));

        PasswordResetCode resetCode = resetCodeRepository.findByUserAndUsedFalse(user)
                .orElseThrow(() -> new RuntimeException("Nenhum pedido de recuperação ativo encontrado"));

        if (resetCode.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new RuntimeException("O código de recuperação expirou");
        }

        if (resetCode.getAttempts() >= 5) {
            resetCode.setUsed(true);
            resetCodeRepository.save(resetCode);
            throw new RuntimeException("Número máximo de tentativas excedido. Solicite um novo código.");
        }

        if (!passwordEncoder.matches(request.getCode(), resetCode.getCodeHash())) {
            resetCode.setAttempts(resetCode.getAttempts() + 1);
            resetCodeRepository.save(resetCode);
            throw new RuntimeException("Código incorreto");
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
            throw new RuntimeException("Token temporário inválido ou expirado");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetCodeRepository.findByUserAndUsedFalse(user).ifPresent(activeCode -> {
            activeCode.setUsed(true);
            resetCodeRepository.save(activeCode);
        });
    }
}