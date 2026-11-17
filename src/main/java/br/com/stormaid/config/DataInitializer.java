package br.com.stormaid.config;

import br.com.stormaid.entity.Role;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.UserRole;
import br.com.stormaid.enums.UserStatus;
import br.com.stormaid.repository.RoleRepository;
import br.com.stormaid.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            Role citizenRole = roleRepository.findByName(UserRole.CITIZEN).orElseGet(() -> {
                Role r = new Role();
                r.setName(UserRole.CITIZEN);
                return roleRepository.save(r);
            });

            Role regulatorRole = roleRepository.findByName(UserRole.REGULATOR).orElseGet(() -> {
                Role r = new Role();
                r.setName(UserRole.REGULATOR);
                return roleRepository.save(r);
            });

            Role adminRole = roleRepository.findByName(UserRole.ADMIN).orElseGet(() -> {
                Role r = new Role();
                r.setName(UserRole.ADMIN);
                return roleRepository.save(r);
            });

            if (userRepository.findByEmail("regulator@stormaid.example").isEmpty()) {
                User regulator = new User();
                regulator.setFullName("Regulator Teste");
                regulator.setEmail("regulator@stormaid.example");
                regulator.setPhoneNumber("+5511977777777");
                regulator.setPassword(passwordEncoder.encode("SenhaForte!2026"));
                regulator.setStatus(UserStatus.ACTIVE);
                regulator.setRoles(Set.of(regulatorRole));
                userRepository.save(regulator);
            }

            if (userRepository.findByEmail("cidadao@stormaid.example").isEmpty()) {
                User citizen = new User();
                citizen.setFullName("Ana Cidadã");
                citizen.setEmail("cidadao@stormaid.example");
                citizen.setPhoneNumber("+5511966666666");
                citizen.setPassword(passwordEncoder.encode("SenhaForte!2026"));
                citizen.setStatus(UserStatus.ACTIVE);
                citizen.setRoles(Set.of(citizenRole));
                userRepository.save(citizen);
            }
        };
    }
}