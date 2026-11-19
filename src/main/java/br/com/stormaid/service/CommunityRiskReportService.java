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

@Service
public class CommunityRiskReportService {

    private final CommunityRiskReportRepository riskReportRepository;

    public CommunityRiskReportService(CommunityRiskReportRepository riskReportRepository) {
        this.riskReportRepository = riskReportRepository;
    }

    @Transactional
    public CommunityRiskReport createRiskReport(User requester, CommunityRiskReportRequest request) {
        validateLocation(request.getLatitude(), request.getLongitude(), request.getAddressText(), request.getReferencePoint());

        CommunityRiskReport riskReport = new CommunityRiskReport();
        riskReport.setRequester(requester);
        riskReport.setCategory(request.getCategory());
        riskReport.setDescription(request.getDescription().trim());
        riskReport.setLocationSource(request.getLocationSource());

        if (request.getLatitude() != null) {
            riskReport.setLatitude(BigDecimal.valueOf(request.getLatitude()));
        }
        if (request.getLongitude() != null) {
            riskReport.setLongitude(BigDecimal.valueOf(request.getLongitude()));
        }

        riskReport.setAddressText(request.getAddressText() != null ? request.getAddressText().trim() : null);
        riskReport.setReferencePoint(request.getReferencePoint() != null ? request.getReferencePoint().trim() : null);
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

        Double effectiveLat = request.getLatitude() != null
                ? request.getLatitude()
                : (report.getLatitude() != null ? report.getLatitude().doubleValue() : null);
        Double effectiveLng = request.getLongitude() != null
                ? request.getLongitude()
                : (report.getLongitude() != null ? report.getLongitude().doubleValue() : null);
        String effectiveAddress = request.getAddressText() != null ? request.getAddressText() : report.getAddressText();
        String effectiveReference = request.getReferencePoint() != null ? request.getReferencePoint() : report.getReferencePoint();

        validateLocation(effectiveLat, effectiveLng, effectiveAddress, effectiveReference);

        if (request.getCategory() != null) {
            report.setCategory(request.getCategory());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            report.setDescription(request.getDescription().trim());
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
            report.setAddressText(request.getAddressText().trim());
        }
        if (request.getReferencePoint() != null) {
            report.setReferencePoint(request.getReferencePoint().trim());
        }

        return riskReportRepository.save(report);
    }

    private void validateLocation(Double latitude, Double longitude, String addressText, String referencePoint) {
        boolean hasCoordinates = latitude != null && longitude != null;
        boolean hasPartialCoordinates = (latitude != null) ^ (longitude != null);
        boolean hasAddress = (addressText != null && !addressText.isBlank())
                || (referencePoint != null && !referencePoint.isBlank());

        if (hasPartialCoordinates) {
            throw new IllegalArgumentException("Informe latitude e longitude juntas");
        }
        if (!hasCoordinates && !hasAddress) {
            throw new IllegalArgumentException("Informe coordenadas (latitude e longitude) ou endereço/ponto de referência");
        }
    }
}