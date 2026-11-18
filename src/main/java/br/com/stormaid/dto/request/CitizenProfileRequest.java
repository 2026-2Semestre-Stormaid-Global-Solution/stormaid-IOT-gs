package br.com.stormaid.dto.request;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CitizenProfileRequest {

    @Valid
    private AddressRequestDto address;

    private String vulnerability;

    private String emergencyContact;
}