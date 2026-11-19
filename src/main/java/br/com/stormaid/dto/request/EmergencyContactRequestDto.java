
package br.com.stormaid.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmergencyContactRequestDto {

    @NotBlank
    @Size(max = 120)
    @JsonAlias("fullName")
    private String name;

    @NotBlank
    @Size(max = 20)
    private String phoneNumber;

    @NotBlank
    @Size(max = 40)
    private String relationship;
}
