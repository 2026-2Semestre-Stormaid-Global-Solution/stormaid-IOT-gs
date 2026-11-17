package br.com.stormaid.dto.response;

import br.com.stormaid.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Set<String> roles;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());

        if (user.getRoles() != null) {
            response.setRoles(user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toSet()));
        }

        response.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}