package br.com.stormaid.controller;

import br.com.stormaid.dto.response.WeatherAlertResponse;
import br.com.stormaid.dto.response.WeatherResponse;
import br.com.stormaid.service.WeatherAlertService;
import br.com.stormaid.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/weather")
@Tag(name = "Weather Controller", description = "Endpoint de previsão meteorológica por localização")
public class WeatherController {

    private final WeatherService weatherService;
    private final WeatherAlertService weatherAlertService;

    public WeatherController(WeatherService weatherService, WeatherAlertService weatherAlertService) {
        this.weatherService = weatherService;
        this.weatherAlertService = weatherAlertService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Obter resumo meteorológico por coordenadas", description = "Consulta a API externa Open-Meteo com base na latitude e longitude informadas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Previsão obtida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Coordenadas inválidas"),
            @ApiResponse(responseCode = "503", description = "Serviço meteorológico indisponível")
    })
    public ResponseEntity<WeatherResponse> getWeatherSummary(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        WeatherResponse response = weatherService.getWeatherSummary(latitude, longitude);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alerts")
    @Operation(summary = "Consultar avisos meteorológicos oficiais", description = "Consulta alertas e avisos oficiais aplicáveis à região informada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Coordenadas inválidas"),
            @ApiResponse(responseCode = "503", description = "Serviço de avisos indisponível")
    })
    public ResponseEntity<List<WeatherAlertResponse>> getWeatherAlerts(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        List<WeatherAlertResponse> alerts = weatherAlertService.getAlerts(latitude, longitude);
        return ResponseEntity.ok(alerts);
    }
}