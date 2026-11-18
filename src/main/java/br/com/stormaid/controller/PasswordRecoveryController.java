package br.com.stormaid.controller;

import br.com.stormaid.dto.request.PasswordRecoveryRequest;
import br.com.stormaid.dto.request.PasswordRecoveryResetRequest;
import br.com.stormaid.dto.request.PasswordRecoveryVerifyRequest;
import br.com.stormaid.dto.response.PasswordRecoveryVerifyResponse;
import br.com.stormaid.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/password-recovery")
public class PasswordRecoveryController {

    private final PasswordRecoveryService recoveryService;

    public PasswordRecoveryController(PasswordRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @PostMapping("/request")
    public ResponseEntity<Void> requestRecovery(@RequestBody @Valid PasswordRecoveryRequest request) {
        recoveryService.requestRecovery(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<PasswordRecoveryVerifyResponse> verifyCode(@RequestBody @Valid PasswordRecoveryVerifyRequest request) {
        PasswordRecoveryVerifyResponse response = recoveryService.verifyCode(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid PasswordRecoveryResetRequest request) {
        recoveryService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}