package br.com.stormaid.dto.request;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CitizenProfileRequest {

    @Valid
    private AddressRequestDto address;

    @Valid
    private VulnerabilityRequestDto vulnerability;

    @Valid
    private EmergencyContactRequestDto emergencyContact;
}