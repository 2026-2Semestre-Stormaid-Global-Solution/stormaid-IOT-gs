package br.com.stormaid.exception;

public class ExternalWeatherServiceException extends RuntimeException {

    public ExternalWeatherServiceException(String message) {
        super(message);
    }

    public ExternalWeatherServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}