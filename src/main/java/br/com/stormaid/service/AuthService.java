package br.com.stormaid.service;

import br.com.stormaid.dto.request.LoginRequest;
import br.com.stormaid.dto.request.RegisterRequest;
import br.com.stormaid.dto.response.LoginResponse;
import br.com.stormaid.dto.response.UserResponse;
import br.com.stormaid.entity.Role;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.UserRole;
import br.com.stormaid.enums.UserStatus;
import br.com.stormaid.repository.RoleRepository;
import br.com.stormaid.repository.UserRepository;
import br.com.stormaid.security.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, TokenService tokenService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase(Locale.ROOT) : "";

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        String normalizedCpf = request.getCpf().replaceAll("\\D", "");
        if (userRepository.existsByCpf(normalizedCpf)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF already exists");
        }

        Role citizenRole = roleRepository.findByName(UserRole.CITIZEN)
                .orElseThrow(() -> new RuntimeException("Default role CITIZEN not found"));

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCpf(normalizedCpf);
        user.setBirthDate(request.getBirthDate());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Collections.singleton(citizenRole));

        User savedUser = userRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email() != null ? request.email().trim().toLowerCase(Locale.ROOT) : "";
        var usernamePassword = new UsernamePasswordAuthenticationToken(normalizedEmail, request.password());
        var auth = authenticationManager.authenticate(usernamePassword);
        var user = (User) auth.getPrincipal();

        String token = tokenService.generateToken(user);

        var userInfo = UserResponse.fromEntity(user);

        return new LoginResponse(token, "Bearer", 7200L, userInfo);
    }
}