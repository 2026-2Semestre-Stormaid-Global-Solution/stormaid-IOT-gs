package br.com.stormaid.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DependentRequest {

    @NotBlank(message = "O nome completo é obrigatório")
    @Size(max = 150, message = "O nome completo deve ter no máximo 150 caracteres")
    private String fullName;

    @NotBlank(message = "O parentesco é obrigatório")
    private String relationship;

    @NotNull(message = "A data de nascimento é obrigatória")
    private LocalDate birthDate;

    private Boolean sameAddressAsGuardian;

    @Valid
    private AddressRequestDto address;

    @Valid
    private VulnerabilityRequestDto vulnerability;
}