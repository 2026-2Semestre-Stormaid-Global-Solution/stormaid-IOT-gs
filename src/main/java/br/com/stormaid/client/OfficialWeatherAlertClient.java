package br.com.stormaid.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Component
public class OfficialWeatherAlertClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public List fetchAlerts(double latitude, double longitude) {
        try {
            return Collections.emptyList();
        } catch (RestClientException e) {
            throw new RuntimeException("Servico de avisos meteorologicos indisponivel.", e);
        }
    }
}