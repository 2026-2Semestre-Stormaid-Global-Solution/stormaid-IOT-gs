package br.com.stormaid.service;

import org.springframework.stereotype.Service;

@Service
public class WeatherMessageService {

    public String interpretCondition(int code) {
        if (code == 0) return "CLEAR";
        if (code >= 1 && code <= 3) return "CLOUDY";
        if (code >= 51 && code <= 67) return "RAIN";
        if (code >= 71 && code <= 77) return "SNOW";
        if (code >= 95) return "THUNDERSTORM";
        return "FAIR";
    }

    public String interpretDescription(int code) {
        if (code == 0) return "Céu limpo";
        if (code >= 1 && code <= 3) return "Parcialmente nublado";
        if (code >= 51 && code <= 65) return "Chuva moderada/fraca";
        if (code >= 66 && code <= 67) return "Chuva forte";
        if (code >= 95) return "Trovoada";
        return "Condição estável";
    }

    public String generateMessage(int code, double precipitation) {
        if (code == 0) {
            return "Céu limpo na sua região.";
        }
        if (code >= 1 && code <= 3) {
            return "Possibilidade de nuvens nas próximas horas.";
        }
        if (code >= 51 && code <= 65 && precipitation < 10) {
            return "Possibilidade de chuva nas próximas horas.";
        }
        if (code >= 66 && code <= 67 || precipitation >= 10) {
            return "Chuva forte prevista para hoje.";
        }
        if (code >= 95) {
            return "Condições de tempestade previstas. Acompanhe os avisos oficiais.";
        }
        return "Previsão obtida com sucesso para a localização.";
    }

    public String getIconIdentifier(int code) {
        if (code == 0) return "icon-clear-sky";
        if (code >= 1 && code <= 3) return "icon-cloudy";
        if (code >= 51 && code <= 67) return "icon-rain";
        if (code >= 95) return "icon-thunderstorm";
        return "icon-default";
    }
}