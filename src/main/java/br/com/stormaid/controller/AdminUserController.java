package br.com.stormaid.controller;

import br.com.stormaid.dto.request.RegulatorProvisionRequest;
import br.com.stormaid.dto.request.ResponderProvisionRequest;
import br.com.stormaid.dto.response.ResponderProfileResponse;
import br.com.stormaid.dto.response.UserResponse;
import br.com.stormaid.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping("/regulators")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> provisionRegulator(@RequestBody @Valid RegulatorProvisionRequest request) {
        UserResponse response = adminUserService.provisionRegulator(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/responders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponderProfileResponse> provisionResponder(@RequestBody @Valid ResponderProvisionRequest request) {
        ResponderProfileResponse response = adminUserService.provisionResponder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}