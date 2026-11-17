package br.com.stormaid.controller;

import br.com.stormaid.dto.request.ResponderAuthorizationRequest;
import br.com.stormaid.dto.response.ResponderProfileResponse;
import br.com.stormaid.entity.User;
import br.com.stormaid.service.ResponderProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalResponderController {

    private final ResponderProfileService responderProfileService;

    public InternalResponderController(ResponderProfileService responderProfileService) {
        this.responderProfileService = responderProfileService;
    }

    @GetMapping
    @PreAuthorize("hasRole('REGULATOR')")
    public ResponseEntity<Page<ResponderProfileResponse>> listResponders(Pageable pageable) {
        return ResponseEntity.ok(responderProfileService.listAll(pageable));
    }

    @PatchMapping("/{id}/authorization")
    @PreAuthorize("hasRole('REGULATOR')")
    public ResponseEntity<ResponderProfileResponse> updateAuthorization(
            @PathVariable Long id,
            @RequestBody ResponderAuthorizationRequest request,
            @AuthenticationPrincipal User regulator) {
        return ResponseEntity.ok(responderProfileService.updateAuthorization(id, request, regulator));
    }
}