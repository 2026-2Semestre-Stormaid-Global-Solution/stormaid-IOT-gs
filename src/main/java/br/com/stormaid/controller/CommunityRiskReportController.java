package br.com.stormaid.controller;

import br.com.stormaid.dto.request.CommunityRiskReportRequest;
import br.com.stormaid.dto.response.CommunityRiskReportResponse;
import br.com.stormaid.entity.User;
import br.com.stormaid.service.CommunityRiskReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/community-risks")
@PreAuthorize("hasRole('CITIZEN')")
public class CommunityRiskReportController {

    private final CommunityRiskReportService riskReportService;

    public CommunityRiskReportController(CommunityRiskReportService riskReportService) {
        this.riskReportService = riskReportService;
    }

    @PostMapping
    public ResponseEntity<CommunityRiskReportResponse> create(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid CommunityRiskReportRequest request
    ) {
        var created = riskReportService.createRiskReport(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommunityRiskReportResponse.fromEntity(created));
    }

    @GetMapping
    public ResponseEntity<Page<CommunityRiskReportResponse>> list(
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        Page<CommunityRiskReportResponse> reports = riskReportService.listByRequester(user, pageable)
                .map(CommunityRiskReportResponse::fromEntity);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityRiskReportResponse> getById(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        var report = riskReportService.findByIdAndRequester(id, user);
        return ResponseEntity.ok(CommunityRiskReportResponse.fromEntity(report));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        riskReportService.deleteRiskReport(id, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommunityRiskReportResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody @Valid CommunityRiskReportRequest request
    ) {
        var updated = riskReportService.updateRiskReport(id, user, request);
        return ResponseEntity.ok(CommunityRiskReportResponse.fromEntity(updated));
    }
}