package br.com.stormaid.service;

import br.com.stormaid.dto.request.CitizenProfileRequest;
import br.com.stormaid.embeddable.Address;
import br.com.stormaid.entity.CitizenProfile;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.MobilityStatus;
import br.com.stormaid.repository.CitizenProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitizenProfileService {

    private final CitizenProfileRepository citizenProfileRepository;

    public CitizenProfileService(CitizenProfileRepository citizenProfileRepository) {
        this.citizenProfileRepository = citizenProfileRepository;
    }

    public CitizenProfile getProfileByUser(User user) {
        return citizenProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    CitizenProfile newProfile = new CitizenProfile();
                    newProfile.setUser(user);

                    if (newProfile.getVulnerability() == null) {
                        br.com.stormaid.embeddable.VulnerabilityInfo vulnerability = new br.com.stormaid.embeddable.VulnerabilityInfo();
                        vulnerability.setMobilityStatus(MobilityStatus.AUTONOMOUS);
                        vulnerability.setHasVisualImpairment(false);
                        vulnerability.setHasHearingImpairment(false);
                        vulnerability.setDependsOnEquipment(false);
                        newProfile.setVulnerability(vulnerability);
                    }

                    if (newProfile.getEmergencyContact() == null) {
                        newProfile.setEmergencyContact(new br.com.stormaid.embeddable.EmergencyContact());
                    }

                    return citizenProfileRepository.save(newProfile);
                });
    }

    @Transactional
    public CitizenProfile updateProfile(User user, CitizenProfileRequest request) {
        CitizenProfile profile = getProfileByUser(user);

        if (request.getAddress() != null) {
            Address address = profile.getAddress() != null ? profile.getAddress() : new Address();

            if (request.getAddress().getPostalCode() != null) {
                address.setPostalCode(request.getAddress().getPostalCode());
            }
            if (request.getAddress().getStreet() != null) {
                address.setStreet(request.getAddress().getStreet());
            }
            if (request.getAddress().getAddressNumber() != null) {
                address.setNumber(request.getAddress().getAddressNumber());
            }
            if (request.getAddress().getComplement() != null) {
                address.setComplement(request.getAddress().getComplement());
            }
            if (request.getAddress().getNeighborhood() != null) {
                address.setNeighborhood(request.getAddress().getNeighborhood());
            }
            if (request.getAddress().getCity() != null) {
                address.setCity(request.getAddress().getCity());
            }
            if (request.getAddress().getState() != null) {
                address.setState(request.getAddress().getState().toUpperCase());
            }
            if (request.getAddress().getReferencePoint() != null) {
                address.setReferencePoint(request.getAddress().getReferencePoint());
            }

            profile.setAddress(address);
        }

        if (request.getVulnerability() != null) {
            var vulnerabilityInfo = profile.getVulnerability() != null ? profile.getVulnerability() : new br.com.stormaid.embeddable.VulnerabilityInfo();
            profile.setVulnerability(vulnerabilityInfo);
        }

        if (request.getEmergencyContact() != null) {
            var emergencyContact = profile.getEmergencyContact() != null ? profile.getEmergencyContact() : new br.com.stormaid.embeddable.EmergencyContact();
            profile.setEmergencyContact(emergencyContact);
        }

        return citizenProfileRepository.save(profile);
    }
}