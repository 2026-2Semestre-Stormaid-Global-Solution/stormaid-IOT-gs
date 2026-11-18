package br.com.stormaid.service;

import br.com.stormaid.client.WeatherApiClient;
import br.com.stormaid.dto.response.WeatherResponse;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class WeatherService {

    private final WeatherApiClient weatherApiClient;

    public WeatherService(WeatherApiClient weatherApiClient) {
        this.weatherApiClient = weatherApiClient;
    }

    public WeatherResponse getWeatherSummary(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude inválida. Deve estar entre -90 e 90.");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude inválida. Deve estar entre -180 e 180.");
        }

        Map<String, Object> response = weatherApiClient.fetchWeatherData(latitude, longitude);

        if (response == null || !response.containsKey("current")) {
            throw new RuntimeException("Resposta incompleta do provedor meteorológico.");
        }

        Map<String, Object> current = (Map<String, Object>) response.get("current");

        Double temperature = ((Number) current.get("temperature_2m")).doubleValue();
        Double feelsLike = ((Number) current.get("apparent_temperature")).doubleValue();
        Double precipitation = ((Number) current.get("precipitation")).doubleValue();
        Integer weatherCode = ((Number) current.get("weather_code")).intValue();
        Double windSpeed = ((Number) current.get("wind_speed_10m")).doubleValue();

        String condition = interpretCondition(weatherCode);
        String description = interpretDescription(weatherCode);

        return WeatherResponse.builder()
                .temperature(temperature)
                .feelsLike(feelsLike)
                .condition(condition)
                .description(description)
                .rainProbability(precipitation > 0 ? 80 : 10)
                .weatherCode(weatherCode)
                .message("Previsão obtida com sucesso para a localização.")
                .source("Open-Meteo")
                .status("AVAILABLE")
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }

    private String interpretCondition(int code) {
        if (code == 0) return "CLEAR";
        if (code >= 1 && code <= 3) return "CLOUDY";
        if (code >= 51 && code <= 67) return "RAIN";
        if (code >= 95) return "THUNDERSTORM";
        return "FAIR";
    }

    private String interpretDescription(int code) {
        if (code == 0) return "Céu limpo";
        if (code >= 1 && code <= 3) return "Parcialmente nublado";
        if (code >= 51 && code <= 67) return "Chuva moderada/fraca";
        if (code >= 95) return "Trovoada";
        return "Condição estável";
    }
}