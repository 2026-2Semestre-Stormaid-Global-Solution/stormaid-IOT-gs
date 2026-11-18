package br.com.stormaid.service;

import br.com.stormaid.dto.request.CommunityRiskReportRequest;
import br.com.stormaid.entity.CommunityRiskReport;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.CommunityRiskStatus;
import br.com.stormaid.repository.CommunityRiskReportRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        riskReport.setRequester(requester);
        riskReport.setCategory(request.getCategory());
        riskReport.setDescription(request.getDescription());
        riskReport.setLocationSource(request.getLocationSource());

        if (request.getLatitude() != null) {
            riskReport.setLatitude(BigDecimal.valueOf(request.getLatitude()));
        }
        if (request.getLongitude() != null) {
            riskReport.setLongitude(BigDecimal.valueOf(request.getLongitude()));
        }

        riskReport.setAddressText(request.getAddressText());
        riskReport.setReferencePoint(request.getReferencePoint());
        riskReport.setStatus(CommunityRiskStatus.REPORTED);

        return riskReportRepository.save(riskReport);
    }

    public Page<CommunityRiskReport> listByRequester(User requester, Pageable pageable) {
        return riskReportRepository.findByRequester(requester, pageable);
    }

    public CommunityRiskReport findByIdAndRequester(Long id, User requester) {
        return riskReportRepository.findById(id)
                .filter(report -> report.getRequester().getId().equals(requester.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Relato de risco não encontrado ou não autorizado"));
    }

    @Transactional
    public void deleteRiskReport(Long id, User requester) {
        CommunityRiskReport report = findByIdAndRequester(id, requester);
        riskReportRepository.delete(report);
    }

    @Transactional
    public CommunityRiskReport updateRiskReport(Long id, User requester, CommunityRiskReportRequest request) {
        CommunityRiskReport report = findByIdAndRequester(id, requester);

        if (request.getCategory() != null) {
            report.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            report.setDescription(request.getDescription());
        }
        if (request.getLocationSource() != null) {
            report.setLocationSource(request.getLocationSource());
        }
        if (request.getLatitude() != null) {
            report.setLatitude(BigDecimal.valueOf(request.getLatitude()));
        }
        if (request.getLongitude() != null) {
            report.setLongitude(BigDecimal.valueOf(request.getLongitude()));
        }
        if (request.getAddressText() != null) {
            report.setAddressText(request.getAddressText());
        }
        if (request.getReferencePoint() != null) {
            report.setReferencePoint(request.getReferencePoint());
        }


        return riskReportRepository.save(report);
    }
}