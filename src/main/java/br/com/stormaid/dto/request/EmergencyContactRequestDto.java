package br.com.stormaid.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmergencyContactRequestDto {

    @Size(max = 120, message = "O nome do contacto de emergência deve ter no máximo 120 caracteres")
    private String name;

    @Size(max = 20, message = "O telefone do contacto de emergência deve ter no máximo 20 caracteres")
    private String phoneNumber;

    @Size(max = 40, message = "O parentesco deve ter no máximo 40 caracteres")
    private String relationship;
}