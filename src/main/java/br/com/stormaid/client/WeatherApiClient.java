package br.com.stormaid.client;

import br.com.stormaid.exception.ExternalWeatherServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class WeatherApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> fetchWeatherData(double latitude, double longitude) {
        String url = UriComponentsBuilder.fromHttpUrl("https://api.open-meteo.com/v1/forecast")
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m")
                .queryParam("hourly", "precipitation_probability")
                .queryParam("timezone", "auto")
                .toUriString();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            return body;
        } catch (RestClientException e) {
            throw new ExternalWeatherServiceException("Erro ao comunicar com o serviço meteorológico externo.", e);
        }
    }
}