package br.com.stormaid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {
    private Double temperature;
    private Double feelsLike;
    private String condition;
    private String description;
    private Integer rainProbability;
    private Integer weatherCode;
    private String message;
    private String source;
    private String status;
    private String updatedAt;
}