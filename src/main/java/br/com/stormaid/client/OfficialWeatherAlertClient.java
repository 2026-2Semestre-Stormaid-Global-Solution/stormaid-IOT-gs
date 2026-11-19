package br.com.stormaid.client;

import br.com.stormaid.exception.ExternalWeatherServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class OfficialWeatherAlertClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.weather.alerts.url:}")
    private String alertsEndpointUrl;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List fetchAlerts(double latitude, double longitude) {
        if (alertsEndpointUrl == null || alertsEndpointUrl.isBlank()) {
            return Collections.emptyList();
        }

        String url = UriComponentsBuilder.fromHttpUrl(alertsEndpointUrl)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .toUriString();

        try {
            Object body = restTemplate.getForObject(url, Object.class);
            if (body == null) {
                return Collections.emptyList();
            }

            if (body instanceof List<?> list) {
                return list.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> (Map) item)
                        .toList();
            }

            if (body instanceof Map<?, ?> map) {
                Object alertsNode = map.get("alertas");
                if (alertsNode == null) {
                    alertsNode = map.get("alerts");
                }
                if (alertsNode instanceof List<?> list) {
                    return list.stream()
                            .filter(Map.class::isInstance)
                            .map(item -> (Map) item)
                            .toList();
                }
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            throw new ExternalWeatherServiceException("Serviço de avisos meteorológicos indisponível.", e);
        }
    }
}