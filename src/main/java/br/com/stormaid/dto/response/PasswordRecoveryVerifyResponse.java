package br.com.stormaid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PasswordRecoveryVerifyResponse {
    private String tempToken;
    private String message;
}