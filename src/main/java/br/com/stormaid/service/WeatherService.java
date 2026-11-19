package br.com.stormaid.service;

import br.com.stormaid.client.WeatherApiClient;
import br.com.stormaid.dto.response.WeatherResponse;
import br.com.stormaid.exception.ExternalWeatherServiceException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private final WeatherApiClient weatherApiClient;
    private final WeatherMessageService weatherMessageService;

    public WeatherService(WeatherApiClient weatherApiClient, WeatherMessageService weatherMessageService) {
        this.weatherApiClient = weatherApiClient;
        this.weatherMessageService = weatherMessageService;
    }

    @SuppressWarnings("unchecked")
    public WeatherResponse getWeatherSummary(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude inválida. Deve estar entre -90 e 90.");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude inválida. Deve estar entre -180 e 180.");
        }

        Map<String, Object> response = weatherApiClient.fetchWeatherData(latitude, longitude);

        if (response == null || !(response.get("current") instanceof Map<?, ?> rawCurrent)) {
            throw new ExternalWeatherServiceException("Resposta incompleta do provedor meteorológico.");
        }

        Map<String, Object> current = (Map<String, Object>) rawCurrent;

        Double temperature = toDouble(current.get("temperature_2m"));
        Double feelsLike = toDouble(current.get("apparent_temperature"));
        Double precipitation = toDouble(current.get("precipitation"));
        Integer weatherCode = toInteger(current.get("weather_code"));

        if (temperature == null || feelsLike == null || precipitation == null || weatherCode == null) {
            throw new ExternalWeatherServiceException("Campos obrigatórios ausentes na resposta atual do Open-Meteo.");
        }

        String currentTime = current.get("time") != null ? current.get("time").toString() : null;
        Integer rainProbability = extractRealRainProbability(response.get("hourly"), currentTime);

        String condition = weatherMessageService.interpretCondition(weatherCode);
        String description = weatherMessageService.interpretDescription(weatherCode);
        String message = weatherMessageService.generateMessage(weatherCode, precipitation);

        return WeatherResponse.builder()
                .temperature(temperature)
                .feelsLike(feelsLike)
                .condition(condition)
                .description(description)
                .rainProbability(rainProbability)
                .weatherCode(weatherCode)
                .message(message)
                .source("Open-Meteo")
                .status("AVAILABLE")
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Integer extractRealRainProbability(Object rawHourly, String currentTime) {
        if (!(rawHourly instanceof Map<?, ?> hourlyMap)) {
            return null;
        }

        Object rawTimes = hourlyMap.get("time");
        Object rawProbs = hourlyMap.get("precipitation_probability");

        if (!(rawTimes instanceof List<?> times) || !(rawProbs instanceof List<?> probs)) {
            return null;
        }

        if (times.isEmpty() || probs.isEmpty()) {
            return null;
        }

        int targetIndex = findClosestHourIndex(times, currentTime);
        if (targetIndex < 0 || targetIndex >= probs.size()) {
            return null;
        }

        return toInteger(probs.get(targetIndex));
    }

    private int findClosestHourIndex(List<?> times, String currentTime) {
        if (currentTime != null && !currentTime.isBlank()) {
            for (int i = 0; i < times.size(); i++) {
                if (currentTime.equals(String.valueOf(times.get(i)))) {
                    return i;
                }
            }
            try {
                LocalDateTime target = LocalDateTime.parse(currentTime);
                int bestIdx = 0;
                long minDiffMinutes = Long.MAX_VALUE;
                for (int i = 0; i < times.size(); i++) {
                    LocalDateTime candidate = LocalDateTime.parse(String.valueOf(times.get(i)));
                    long diff = Math.abs(java.time.Duration.between(target, candidate).toMinutes());
                    if (diff < minDiffMinutes) {
                        minDiffMinutes = diff;
                        bestIdx = i;
                    }
                }
                return bestIdx;
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private Double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private Integer toInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}