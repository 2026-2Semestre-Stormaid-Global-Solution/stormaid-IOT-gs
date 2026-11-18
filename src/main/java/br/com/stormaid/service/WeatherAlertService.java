package br.com.stormaid.service;

import br.com.stormaid.client.OfficialWeatherAlertClient;
import br.com.stormaid.dto.response.WeatherAlertResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class WeatherAlertService {

    private final OfficialWeatherAlertClient alertClient;

    public WeatherAlertService(OfficialWeatherAlertClient alertClient) {
        this.alertClient = alertClient;
    }

    public List<WeatherAlertResponse> getAlerts(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude invalida. Deve estar entre -90 e 90.");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude invalida. Deve estar entre -180 e 180.");
        }

        List rawAlerts = alertClient.fetchAlerts(latitude, longitude);

        if (rawAlerts == null || rawAlerts.isEmpty()) {
            return Collections.emptyList();
        }

        return (List<WeatherAlertResponse>) (List<?>) rawAlerts.stream().map(item -> {
            Map alert = (Map) item;
            return WeatherAlertResponse.builder()
                    .phenomenon((String) alert.getOrDefault("phenomenon", "Desconhecido"))
                    .severity((String) alert.getOrDefault("severity", "MODERATE"))
                    .description((String) alert.getOrDefault("description", "Sem descricao"))
                    .period((String) alert.getOrDefault("period", "Atual"))
                    .issuingBody((String) alert.getOrDefault("issuingBody", "Orgao Oficial"))
                    .source("Defesa Civil / INMET")
                    .status("ACTIVE")
                    .build();
        }).toList();
    }
}