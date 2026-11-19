package br.com.stormaid.controller;

import br.com.stormaid.dto.response.AppVersionResponse;
import br.com.stormaid.service.AppVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app")
@Tag(name = "App Info", description = "Informações de versão e build da aplicação")
public class AppVersionController {

    private final AppVersionService appVersionService;

    public AppVersionController(AppVersionService appVersionService) {
        this.appVersionService = appVersionService;
    }

    @GetMapping("/info")
    @Operation(summary = "Obtém informações de versão e último commit implantado")
    public ResponseEntity<AppVersionResponse> getAppInfo() {
        return ResponseEntity.ok(appVersionService.getAppInfo());
    }
}