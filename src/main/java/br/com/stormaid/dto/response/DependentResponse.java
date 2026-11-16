package br.com.stormaid.dto.response;

import br.com.stormaid.embeddable.Address;
import br.com.stormaid.embeddable.VulnerabilityInfo;
import br.com.stormaid.entity.Dependent;
import br.com.stormaid.enums.RelationshipType;

import java.time.Instant;
import java.time.LocalDate;

public record DependentResponse(
        Long id,
        Long guardianId,
        String fullName,
        RelationshipType relationship,
        LocalDate birthDate,
        Boolean sameAddressAsGuardian,
        Address address,
        VulnerabilityInfo vulnerability,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static DependentResponse fromEntity(Dependent dependent) {
        return new DependentResponse(
                dependent.getId(),
                dependent.getGuardian().getId(),
                dependent.getFullName(),
                dependent.getRelationship(),
                dependent.getBirthDate(),
                dependent.getSameAddressAsGuardian(),
                dependent.getAddress(),
                dependent.getVulnerability(),
                dependent.getActive(),
                dependent.getCreatedAt(),
                dependent.getUpdatedAt()
        );
    }
}