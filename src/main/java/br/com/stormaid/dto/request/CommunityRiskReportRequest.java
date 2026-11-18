package br.com.stormaid.dto.request;

import br.com.stormaid.enums.CommunityRiskCategory;
import br.com.stormaid.enums.LocationSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommunityRiskReportRequest {

    @NotNull(message = "A categoria do risco é obrigatória")
    private CommunityRiskCategory category;

    @NotBlank(message = "A descrição é obrigatória")
    private String description;

    @NotNull(message = "A latitude é obrigatória")
    private Double latitude;

    @NotNull(message = "A longitude é obrigatória")
    private Double longitude;

    private LocationSource locationSource;

    private String addressText;

    private String referencePoint;
}