package br.com.stormaid.dto.response;

import br.com.stormaid.embeddable.Address;
import br.com.stormaid.embeddable.EmergencyContact;
import br.com.stormaid.embeddable.VulnerabilityInfo;
import br.com.stormaid.entity.CitizenProfile;

import java.time.Instant;

public record CitizenProfileResponse(
        Long id,
        Long userId,
        Address address,
        VulnerabilityInfo vulnerability,
        EmergencyContact emergencyContact,
        Instant updatedAt
) {
    public static CitizenProfileResponse fromEntity(CitizenProfile profile) {
        return new CitizenProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getAddress(),
                profile.getVulnerability(),
                profile.getEmergencyContact(),
                profile.getUpdatedAt()
        );
    }
}