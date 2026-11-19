package br.com.stormaid.service;

import br.com.stormaid.dto.request.AddressRequestDto;
import br.com.stormaid.dto.request.CitizenProfileRequest;
import br.com.stormaid.dto.request.EmergencyContactRequestDto;
import br.com.stormaid.dto.request.VulnerabilityRequestDto;
import br.com.stormaid.embeddable.Address;
import br.com.stormaid.embeddable.EmergencyContact;
import br.com.stormaid.embeddable.VulnerabilityInfo;
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

    @Transactional(readOnly = true)
    public CitizenProfile getProfileByUser(User user) {
        return citizenProfileRepository.findByUser(user)
                .orElseGet(() -> buildDefaultProfile(user));
    }

    @Transactional
    public CitizenProfile updateProfile(User user, CitizenProfileRequest request) {
        CitizenProfile profile = citizenProfileRepository.findByUser(user)
                .orElseGet(() -> buildDefaultProfile(user));

        profile.setUser(user);

        if (request.getAddress() != null) {
            profile.setAddress(mergeAddress(profile.getAddress(), request.getAddress()));
        }

        if (request.getVulnerability() != null) {
            profile.setVulnerability(mergeVulnerability(profile.getVulnerability(), request.getVulnerability()));
        } else if (profile.getVulnerability() == null) {
            profile.setVulnerability(defaultVulnerability());
        }

        if (request.getEmergencyContact() != null) {
            profile.setEmergencyContact(
                    mergeOrClearEmergencyContact(profile.getEmergencyContact(), request.getEmergencyContact())
            );
        }

        return citizenProfileRepository.save(profile);
    }

    private CitizenProfile buildDefaultProfile(User user) {
        CitizenProfile profile = new CitizenProfile();
        profile.setUser(user);
        profile.setAddress(new Address());
        profile.setVulnerability(defaultVulnerability());
        profile.setEmergencyContact(new EmergencyContact());
        return profile;
    }

    private VulnerabilityInfo defaultVulnerability() {
        VulnerabilityInfo vulnerability = new VulnerabilityInfo();
        vulnerability.setMobilityStatus(MobilityStatus.AUTONOMOUS);
        vulnerability.setHasVisualImpairment(false);
        vulnerability.setHasHearingImpairment(false);
        vulnerability.setDependsOnEquipment(false);
        return vulnerability;
    }

    private Address mergeAddress(Address current, AddressRequestDto req) {
        Address address = current != null ? current : new Address();

        if (req.getPostalCode() != null) {
            address.setPostalCode(req.getPostalCode());
        }
        if (req.getStreet() != null) {
            address.setStreet(req.getStreet());
        }
        if (req.getAddressNumber() != null) {
            address.setNumber(req.getAddressNumber());
        }
        if (req.getComplement() != null) {
            address.setComplement(req.getComplement());
        }
        if (req.getNeighborhood() != null) {
            address.setNeighborhood(req.getNeighborhood());
        }
        if (req.getCity() != null) {
            address.setCity(req.getCity());
        }
        if (req.getState() != null) {
            address.setState(req.getState().toUpperCase());
        }
        if (req.getReferencePoint() != null) {
            address.setReferencePoint(req.getReferencePoint());
        }

        return address;
    }

    private VulnerabilityInfo mergeVulnerability(VulnerabilityInfo current, VulnerabilityRequestDto req) {
        VulnerabilityInfo vulnerability = current != null ? current : defaultVulnerability();

        if (req.getMobilityStatus() != null) {
            vulnerability.setMobilityStatus(req.getMobilityStatus());
        }
        if (req.getHasVisualImpairment() != null) {
            vulnerability.setHasVisualImpairment(req.getHasVisualImpairment());
        }
        if (req.getHasHearingImpairment() != null) {
            vulnerability.setHasHearingImpairment(req.getHasHearingImpairment());
        }
        if (req.getDependsOnEquipment() != null) {
            vulnerability.setDependsOnEquipment(req.getDependsOnEquipment());
        }
        if (req.getEquipmentDescription() != null) {
            vulnerability.setEquipmentDescription(req.getEquipmentDescription().isBlank() ? null : req.getEquipmentDescription());
        }
        if (req.getAccessibilityNotes() != null) {
            vulnerability.setAccessibilityNotes(req.getAccessibilityNotes().isBlank() ? null : req.getAccessibilityNotes());
        }

        return vulnerability;
    }

    private EmergencyContact mergeOrClearEmergencyContact(
            EmergencyContact current,
            EmergencyContactRequestDto req
    ) {
        boolean shouldClearAll = isBlank(req.getName())
                && isBlank(req.getPhoneNumber())
                && isBlank(req.getRelationship());

        if (shouldClearAll) {
            EmergencyContact cleared = new EmergencyContact();
            cleared.setName(null);
            cleared.setPhoneNumber(null);
            cleared.setRelationship(null);
            return cleared;
        }

        EmergencyContact contact = current != null ? current : new EmergencyContact();

        if (req.getName() != null) {
            contact.setName(req.getName().isBlank() ? null : req.getName());
        }
        if (req.getPhoneNumber() != null) {
            contact.setPhoneNumber(req.getPhoneNumber().isBlank() ? null : req.getPhoneNumber());
        }
        if (req.getRelationship() != null) {
            contact.setRelationship(req.getRelationship().isBlank() ? null : req.getRelationship());
        }

        return contact;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}