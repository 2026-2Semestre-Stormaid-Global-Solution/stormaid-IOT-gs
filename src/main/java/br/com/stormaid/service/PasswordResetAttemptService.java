package br.com.stormaid.service;

import br.com.stormaid.entity.PasswordResetCode;
import br.com.stormaid.repository.PasswordResetCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetAttemptService {

    public static final int MAX_ATTEMPTS = 5;

    private final PasswordResetCodeRepository resetCodeRepository;

    public PasswordResetAttemptService(PasswordResetCodeRepository resetCodeRepository) {
        this.resetCodeRepository = resetCodeRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int registerFailedAttempt(Long resetCodeId) {
        PasswordResetCode resetCode = resetCodeRepository.findById(resetCodeId)
                .orElseThrow(() -> new IllegalStateException("Código de recuperação não encontrado"));

        int nextAttempts = resetCode.getAttempts() + 1;
        resetCode.setAttempts(nextAttempts);

        if (nextAttempts >= MAX_ATTEMPTS) {
            resetCode.setUsed(true);
        }

        resetCodeRepository.saveAndFlush(resetCode);
        return nextAttempts;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsUsed(Long resetCodeId) {
        resetCodeRepository.findById(resetCodeId).ifPresent(code -> {
            code.setUsed(true);
            resetCodeRepository.saveAndFlush(code);
        });
    }
}