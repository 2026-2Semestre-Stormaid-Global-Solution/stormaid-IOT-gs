package br.com.stormaid.dto.request;

import br.com.stormaid.enums.CommunityRiskCategory;
import br.com.stormaid.enums.LocationSource;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommunityRiskReportRequest {

    @NotNull(message = "A categoria do risco é obrigatória")
    private CommunityRiskCategory category;

    @NotBlank(message = "A descrição é obrigatória")
    @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
    private String description;

    @DecimalMin(value = "-90.0", message = "A latitude deve ser maior ou igual a -90.0")
    @DecimalMax(value = "90.0", message = "A latitude deve ser menor ou igual a 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "A longitude deve ser maior ou igual a -180.0")
    @DecimalMax(value = "180.0", message = "A longitude deve ser menor ou igual a 180.0")
    private Double longitude;

    private LocationSource locationSource;

    @Size(max = 300, message = "O endereço deve ter no máximo 300 caracteres")
    private String addressText;

    @Size(max = 250, message = "O ponto de referência deve ter no máximo 250 caracteres")
    private String referencePoint;
}