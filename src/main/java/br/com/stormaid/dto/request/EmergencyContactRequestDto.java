<<<<<<< HEAD
package br.com.stormaid.dto.request;

=======

package br.com.stormaid.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
>>>>>>> e42fded058042be3b877346909cf6ee2e27f79c7
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmergencyContactRequestDto {

<<<<<<< HEAD
    @Size(max = 120, message = "O nome do contacto de emergência deve ter no máximo 120 caracteres")
    private String name;

    @Size(max = 20, message = "O telefone do contacto de emergência deve ter no máximo 20 caracteres")
    private String phoneNumber;

    @Size(max = 40, message = "O parentesco deve ter no máximo 40 caracteres")
    private String relationship;
}
=======
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
>>>>>>> e42fded058042be3b877346909cf6ee2e27f79c7
