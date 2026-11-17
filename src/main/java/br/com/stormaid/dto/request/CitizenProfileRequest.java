package br.com.stormaid.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CitizenProfileRequest {
    private String address;
    private String vulnerability;
    private String emergencyContact;
}