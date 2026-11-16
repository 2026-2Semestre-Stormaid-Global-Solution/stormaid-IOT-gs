package br.com.stormaid.dto.response;

import br.com.stormaid.entity.CommunityRiskReport;
import br.com.stormaid.enums.CommunityRiskCategory;
import br.com.stormaid.enums.CommunityRiskStatus;
import br.com.stormaid.enums.LocationSource;

import java.math.BigDecimal;
import java.time.Instant;

public record CommunityRiskReportResponse(
        Long id,
        Long requesterUserId,
        CommunityRiskCategory category,
        String description,
        LocationSource locationSource,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressText,
        String referencePoint,
        CommunityRiskStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommunityRiskReportResponse fromEntity(CommunityRiskReport report) {
        return new CommunityRiskReportResponse(
                report.getId(),
                report.getRequester().getId(),
                report.getCategory(),
                report.getDescription(),
                report.getLocationSource(),
                report.getLatitude(),
                report.getLongitude(),
                report.getAddressText(),
                report.getReferencePoint(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}