package br.com.stormaid.service;

import br.com.stormaid.dto.request.ResponderProvisionRequest;
import br.com.stormaid.dto.response.ResponderProfileResponse;
import br.com.stormaid.entity.ResponderProfile;
import br.com.stormaid.entity.Role;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.ResponderVerificationStatus;
import br.com.stormaid.enums.UserRole;
import br.com.stormaid.enums.UserStatus;
import br.com.stormaid.repository.ResponderProfileRepository;
import br.com.stormaid.repository.RoleRepository;
import br.com.stormaid.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Locale;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ResponderProfileRepository responderProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            ResponderProfileRepository responderProfileRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.responderProfileRepository = responderProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ResponderProfileResponse provisionResponder(ResponderProvisionRequest request) {

        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("E-mail já cadastrado no sistema.");
        }

        if (responderProfileRepository.existsByInstitutionTypeAndBadgeNumber(request.getInstitutionType(), request.getBadgeNumber())) {
            throw new IllegalArgumentException("Já existe um socorrista cadastrado com esta matrícula para a instituição informada.");
        }

        Role responderRole = roleRepository.findByName(UserRole.RESPONDER)
                .orElseThrow(() -> new RuntimeException("Role RESPONDER não encontrada"));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(request.getPhoneNumber());

        user.setPassword(passwordEncoder.encode(request.getTemporaryPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Collections.singleton(responderRole));

        User savedUser = userRepository.save(user);

        ResponderProfile profile = new ResponderProfile();
        profile.setUser(savedUser);
        profile.setBadgeNumber(request.getBadgeNumber());
        profile.setInstitutionType(request.getInstitutionType());
        profile.setVerificationStatus(ResponderVerificationStatus.PENDING);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());

        ResponderProfile savedProfile = responderProfileRepository.save(profile);

        return mapToResponse(savedProfile);
    }

    private ResponderProfileResponse mapToResponse(ResponderProfile profile) {
        ResponderProfileResponse res = new ResponderProfileResponse();
        res.setId(profile.getId());
        res.setUserId(profile.getUser().getId());
        res.setFullName(profile.getUser().getFullName());
        res.setEmail(profile.getUser().getEmail());
        res.setPhoneNumber(profile.getUser().getPhoneNumber());
        res.setBadgeNumber(profile.getBadgeNumber());
        res.setInstitutionType(profile.getInstitutionType());
        res.setVerificationStatus(profile.getVerificationStatus());
        res.setAuthorizedAt(profile.getAuthorizedAt());
        res.setCreatedAt(profile.getCreatedAt());
        if (profile.getAuthorizedBy() != null) {
            res.setAuthorizedByUserId(profile.getAuthorizedBy().getId());
        }
        return res;
    }
}