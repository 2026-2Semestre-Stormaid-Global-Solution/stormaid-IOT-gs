package br.com.stormaid.controller;

import br.com.stormaid.dto.request.ResponderProvisionRequest;
import br.com.stormaid.dto.response.ResponderProfileResponse;
import br.com.stormaid.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping("/responders")
    public ResponseEntity<ResponderProfileResponse> provisionResponder(@RequestBody @Valid ResponderProvisionRequest request) {
        ResponderProfileResponse response = adminUserService.provisionResponder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}