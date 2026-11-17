package br.com.stormaid.dto.response;

import br.com.stormaid.entity.ResponderProfile;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ResponderProfileResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String badgeNumber;
    private String institutionType;
    private String verificationStatus;
    private Instant authorizedAt;
    private Instant createdAt;
    private Long authorizedByUserId;

    public static ResponderProfileResponse fromEntity(ResponderProfile profile) {
        ResponderProfileResponse res = new ResponderProfileResponse();
        res.setId(profile.getId());
        if (profile.getUser() != null) {
            res.setUserId(profile.getUser().getId());
            res.setFullName(profile.getUser().getFullName());
            res.setEmail(profile.getUser().getEmail());
            res.setPhoneNumber(profile.getUser().getPhoneNumber());
        }
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
}