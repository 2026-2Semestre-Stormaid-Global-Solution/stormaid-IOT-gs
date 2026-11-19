package br.com.stormaid.service;

import br.com.stormaid.dto.request.RegisterCompleteRequest;
import br.com.stormaid.dto.response.LoginResponse;
import br.com.stormaid.dto.response.UserResponse;
import br.com.stormaid.embeddable.Address;
import br.com.stormaid.embeddable.EmergencyContact;
import br.com.stormaid.embeddable.VulnerabilityInfo;
import br.com.stormaid.entity.CitizenProfile;
import br.com.stormaid.entity.User;
import br.com.stormaid.repository.CitizenProfileRepository;
import br.com.stormaid.repository.UserRepository;
import br.com.stormaid.security.TokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitizenRegistrationService {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final TokenService tokenService;

    public CitizenRegistrationService(
            AuthService authService,
            UserRepository userRepository,
            CitizenProfileRepository citizenProfileRepository,
            TokenService tokenService
    ) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.citizenProfileRepository = citizenProfileRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public LoginResponse registerComplete(RegisterCompleteRequest request) {

        UserResponse createdUser = authService.register(
                request.getAccount()
        );

        User user = userRepository.findById(createdUser.getId())
                .orElseThrow(() ->
                        new IllegalStateException("Usuário criado não encontrado.")
                );

        var addressRequest = request.getAddress();

        Address address = new Address();
        address.setPostalCode(addressRequest.getPostalCode());
        address.setStreet(addressRequest.getStreet());
        address.setNumber(addressRequest.getAddressNumber());
        address.setComplement(addressRequest.getComplement());
        address.setNeighborhood(addressRequest.getNeighborhood());
        address.setCity(addressRequest.getCity());
        address.setState(addressRequest.getState().toUpperCase());
        address.setReferencePoint(addressRequest.getReferencePoint());

        var vulnerabilityRequest = request.getVulnerability();

        VulnerabilityInfo vulnerability = new VulnerabilityInfo();
        vulnerability.setMobilityStatus(
                vulnerabilityRequest.getMobilityStatus()
        );
        vulnerability.setHasVisualImpairment(
                vulnerabilityRequest.getHasVisualImpairment()
        );
        vulnerability.setHasHearingImpairment(
                vulnerabilityRequest.getHasHearingImpairment()
        );
        vulnerability.setHasSpeechImpairment(
                vulnerabilityRequest.getHasSpeechImpairment()
        );
        vulnerability.setHasCognitiveImpairment(
                vulnerabilityRequest.getHasCognitiveImpairment()
        );
        vulnerability.setDependsOnEquipment(
                vulnerabilityRequest.getDependsOnEquipment()
        );
        vulnerability.setEquipmentDescription(
                vulnerabilityRequest.getEquipmentDescription()
        );
        vulnerability.setEquipmentInformationDeclined(
                vulnerabilityRequest.getEquipmentInformationDeclined()
        );

        EmergencyContact emergencyContact = null;

        if (!Boolean.TRUE.equals(request.getNoEmergencyContact())) {
            var contactRequest = request.getEmergencyContact();

            emergencyContact = new EmergencyContact();
            emergencyContact.setName(contactRequest.getName().trim());
            emergencyContact.setPhoneNumber(
                    contactRequest.getPhoneNumber()
            );
            emergencyContact.setRelationship(
                    contactRequest.getRelationship()
            );
        }

        CitizenProfile profile = new CitizenProfile();
        profile.setUser(user);
        profile.setAddress(address);
        profile.setVulnerability(vulnerability);
        profile.setEmergencyContact(emergencyContact);

        citizenProfileRepository.saveAndFlush(profile);

        String accessToken = tokenService.generateToken(user);

        return new LoginResponse(
                accessToken,
                "Bearer",
                7200L,
                UserResponse.fromEntity(user)
        );
    }
}
