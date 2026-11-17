package br.com.stormaid.service;

import br.com.stormaid.dto.request.ResponderAuthorizationRequest;
import br.com.stormaid.dto.response.ResponderEligibilityResponse;
import br.com.stormaid.dto.response.ResponderProfileResponse;
import br.com.stormaid.entity.ResponderProfile;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.ResponderVerificationStatus;
import br.com.stormaid.repository.ResponderProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ResponderProfileService {

    private final ResponderProfileRepository responderProfileRepository;

    public ResponderProfileService(ResponderProfileRepository responderProfileRepository) {
        this.responderProfileRepository = responderProfileRepository;
    }

    public Page<ResponderProfileResponse> listAll(Pageable pageable) {
        return responderProfileRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public ResponderProfileResponse updateAuthorization(Long id, ResponderAuthorizationRequest request, User regulator) {
        ResponderProfile profile = responderProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Perfil de socorrista não encontrado com ID: " + id));

        profile.setVerificationStatus(request.getVerificationStatus());
        profile.setUpdatedAt(Instant.now());

        profile.setAuthorizedBy(regulator);

        if (request.getVerificationStatus() == ResponderVerificationStatus.APPROVED) {
            profile.setAuthorizedAt(Instant.now());
        }

        ResponderProfile updated = responderProfileRepository.save(profile);
        return mapToResponse(updated);
    }

    public ResponderEligibilityResponse checkEligibility(Long userId) {
        ResponderProfile profile = responderProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Perfil não encontrado para o utilizador: " + userId));

        User user = profile.getUser();

        boolean eligible = user.getStatus() == br.com.stormaid.enums.UserStatus.ACTIVE &&
                profile.getVerificationStatus() == ResponderVerificationStatus.APPROVED;

        ResponderEligibilityResponse response = new ResponderEligibilityResponse();
        response.setUserId(user.getId());
        response.setResponderProfileId(profile.getId());
        response.setEligible(eligible);
        response.setInstitutionType(profile.getInstitutionType());
        response.setUserStatus(user.getStatus());
        response.setVerificationStatus(profile.getVerificationStatus());

        return response;
    }

    private ResponderProfileResponse mapToResponse(ResponderProfile profile) {
        ResponderProfileResponse res = new ResponderProfileResponse();
        res.setId(profile.getId());
        res.setUserId(profile.getUser().getId());
        res.setFullName(profile.getUser().getFullName());
        res.setEmail(profile.getUser().getEmail());
        res.setPhoneNumber(profile.getUser().getPhoneNumber());
        res.setBadgeNumber(profile.getBadgeNumber());
        res.setInstitutionType(profile.getInstitutionType() != null ? profile.getInstitutionType().name() : null);
        res.setVerificationStatus(profile.getVerificationStatus() != null ? profile.getVerificationStatus().name() : null);
        res.setAuthorizedAt(profile.getAuthorizedAt());
        res.setCreatedAt(profile.getCreatedAt());
        if (profile.getAuthorizedBy() != null) {
            res.setAuthorizedByUserId(profile.getAuthorizedBy().getId());
        }
        return res;
    }

    public ResponderProfile getProfileByUser(User user) {
        return responderProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Perfil de socorrista não encontrado para o usuário atual"));
    }
}