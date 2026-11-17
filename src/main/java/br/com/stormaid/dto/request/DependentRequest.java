package br.com.stormaid.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DependentRequest {
    @NotBlank(message = "O nome completo é obrigatório")
    private String fullName;

    @NotBlank(message = "O parentesco é obrigatório")
    private String relationship;

    @NotNull(message = "A data de nascimento é obrigatória")
    private LocalDate birthDate;
}