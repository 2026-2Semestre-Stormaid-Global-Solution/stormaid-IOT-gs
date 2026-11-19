
package br.com.stormaid.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterCompleteRequest {

    @Valid
    @NotNull
    private RegisterRequest account;

    @Valid
    @NotNull
    private AddressRequestDto address;

    @Valid
    @NotNull
    private VulnerabilityRequestDto vulnerability;

    @NotNull
    private Boolean noEmergencyContact;

    @Valid
    private EmergencyContactRequestDto emergencyContact;

    @AssertTrue(message = "Dados do contato de emergência inconsistentes")
    public boolean isEmergencyContactValid() {
        if (noEmergencyContact == null) {
            return false;
        }

        if (noEmergencyContact) {
            return emergencyContact == null;
        }

        return emergencyContact != null;
    }
}
