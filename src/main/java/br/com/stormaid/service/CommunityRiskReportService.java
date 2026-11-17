package br.com.stormaid.service;

import br.com.stormaid.dto.request.CommunityRiskReportRequest;
import br.com.stormaid.entity.CommunityRiskReport;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.CommunityRiskStatus;
import br.com.stormaid.repository.CommunityRiskReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CommunityRiskReportService {

    private final CommunityRiskReportRepository riskReportRepository;

    public CommunityRiskReportService(CommunityRiskReportRepository riskReportRepository) {
        this.riskReportRepository = riskReportRepository;
    }

    @Transactional
    public CommunityRiskReport createRiskReport(User requester, CommunityRiskReportRequest request) {
        CommunityRiskReport riskReport = new CommunityRiskReport();

        riskReport.setDescription(request.getDescription());

        if (request.getLatitude() != null) {
            riskReport.setLatitude(new java.math.BigDecimal(request.getLatitude().toString()));
        }
        if (request.getLongitude() != null) {
            riskReport.setLongitude(new java.math.BigDecimal(request.getLongitude().toString()));
        }

        riskReport.setRequester(requester);
        riskReport.setStatus(br.com.stormaid.enums.CommunityRiskStatus.REPORTED);
        riskReport.setCreatedAt(java.time.Instant.now());
        riskReport.setUpdatedAt(java.time.Instant.now());

        return riskReportRepository.save(riskReport);
    }

    public Page<CommunityRiskReport> listByRequester(User requester, Pageable pageable) {
        return riskReportRepository.findByRequester(requester, pageable);
    }

    public CommunityRiskReport findByIdAndRequester(Long id, User requester) {
        return riskReportRepository.findById(id)
                .filter(report -> report.getRequester().getId().equals(requester.getId()))
                .orElseThrow(() -> new RuntimeException("Relato de risco não encontrado ou não autorizado"));
    }

    @Transactional
    public void deleteRiskReport(Long id, User requester) {
        CommunityRiskReport report = findByIdAndRequester(id, requester);
        riskReportRepository.delete(report);
    }

    @Transactional
    public CommunityRiskReport updateRiskReport(Long id, User requester, CommunityRiskReportRequest request) {
        CommunityRiskReport report = findByIdAndRequester(id, requester);

        if (request.getDescription() != null) {
            report.setDescription(request.getDescription());
        }
        if (request.getLatitude() != null) {
            report.setLatitude(new java.math.BigDecimal(request.getLatitude().toString()));
        }
        if (request.getLongitude() != null) {
            report.setLongitude(new java.math.BigDecimal(request.getLongitude().toString()));
        }

        report.setUpdatedAt(Instant.now());

        return riskReportRepository.save(report);
    }
}