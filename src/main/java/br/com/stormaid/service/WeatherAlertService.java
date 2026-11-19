package br.com.stormaid.service;

import br.com.stormaid.client.OfficialWeatherAlertClient;
import br.com.stormaid.dto.response.WeatherAlertResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class WeatherAlertService {

    private final OfficialWeatherAlertClient alertClient;

    public WeatherAlertService(OfficialWeatherAlertClient alertClient) {
        this.alertClient = alertClient;
    }

    @SuppressWarnings("unchecked")
    public List<WeatherAlertResponse> getAlerts(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude inválida. Deve estar entre -90 e 90.");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude inválida. Deve estar entre -180 e 180.");
        }

        List rawAlerts = alertClient.fetchAlerts(latitude, longitude);

        if (rawAlerts == null || rawAlerts.isEmpty()) {
            return Collections.emptyList();
        }

        return ((List<?>) rawAlerts).stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .map(this::normalizeAlert)
                .toList();
    }

    private WeatherAlertResponse normalizeAlert(Map<String, Object> alert) {
        String phenomenon = firstNonBlank(
                asString(alert.get("phenomenon")),
                asString(alert.get("evento")),
                "Aviso meteorológico"
        );

        String severity = normalizeSeverity(firstNonBlank(
                asString(alert.get("severity")),
                asString(alert.get("severidade")),
                "MODERATE"
        ));

        String description = firstNonBlank(
                asString(alert.get("description")),
                asString(alert.get("descricao")),
                "Sem descrição detalhada."
        );

        String period = firstNonBlank(
                asString(alert.get("period")),
                asString(alert.get("periodo")),
                "Vigente"
        );

        String issuingBody = firstNonBlank(
                asString(alert.get("issuingBody")),
                asString(alert.get("orgaoEmissor")),
                "Defesa Civil / INMET"
        );

        String source = firstNonBlank(
                asString(alert.get("source")),
                "Fonte Oficial"
        );

        String status = Objects.toString(firstNonBlank(
                asString(alert.get("status")),
                "ACTIVE"
        ), "ACTIVE").toUpperCase(Locale.ROOT);

        return WeatherAlertResponse.builder()
                .phenomenon(phenomenon)
                .severity(severity)
                .description(description)
                .period(period)
                .issuingBody(issuingBody)
                .source(source)
                .status(status)
                .build();
    }

    private String normalizeSeverity(String raw) {
        if (raw == null || raw.isBlank()) {
            return "MODERATE";
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "MINOR", "BAIXA", "LOW", "AMARELO", "YELLOW" -> "MINOR";
            case "SEVERE", "ALTA", "HIGH", "VERMELHO", "RED" -> "SEVERE";
            case "EXTREME", "EXTREMA" -> "EXTREME";
            default -> "MODERATE";
        };
    }

    private String asString(Object value) {
        return value != null ? value.toString().trim() : null;
    }

    private String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c;
            }
        }
        return null;
    }
}