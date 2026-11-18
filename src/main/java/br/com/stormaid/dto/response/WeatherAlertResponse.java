package br.com.stormaid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherAlertResponse {
    private String phenomenon;
    private String severity;
    private String description;
    private String period;
    private String issuingBody;
    private String source;
    private String status;
}