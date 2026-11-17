package br.com.stormaid.service;

import br.com.stormaid.dto.request.CitizenProfileRequest;
import br.com.stormaid.entity.CitizenProfile;
import br.com.stormaid.entity.User;
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
                    return citizenProfileRepository.save(newProfile);
                });
    }

    @Transactional
    public CitizenProfile updateProfile(User user, CitizenProfileRequest request) {
        CitizenProfile profile = getProfileByUser(user);

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