package br.com.stormaid.service;

import br.com.stormaid.client.WeatherApiClient;
import br.com.stormaid.dto.response.WeatherResponse;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class WeatherService {

    private final WeatherApiClient weatherApiClient;
    private final WeatherMessageService weatherMessageService;

    public WeatherService(WeatherApiClient weatherApiClient, WeatherMessageService weatherMessageService) {
        this.weatherApiClient = weatherApiClient;
        this.weatherMessageService = weatherMessageService;
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

        String condition = weatherMessageService.interpretCondition(weatherCode);
        String description = weatherMessageService.interpretDescription(weatherCode);
        String message = weatherMessageService.generateMessage(weatherCode, precipitation);

        return WeatherResponse.builder()
                .temperature(temperature)
                .feelsLike(feelsLike)
                .condition(condition)
                .description(description)
                .rainProbability(precipitation > 0 ? 80 : 10)
                .weatherCode(weatherCode)
                .message(message)
                .source("Open-Meteo")
                .status("AVAILABLE")
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }
}