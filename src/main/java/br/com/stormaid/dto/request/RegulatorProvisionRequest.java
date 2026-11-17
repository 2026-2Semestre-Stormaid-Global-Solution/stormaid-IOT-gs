package br.com.stormaid.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegulatorProvisionRequest {
    @NotBlank
    @Size(min = 3, max = 150)
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 20)
    private String phoneNumber;

    @NotBlank
    @Size(min = 6, max = 100)
    private String temporaryPassword;
}