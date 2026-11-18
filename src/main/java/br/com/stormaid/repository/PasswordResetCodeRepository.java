package br.com.stormaid.repository;

import br.com.stormaid.entity.PasswordResetCode;
import br.com.stormaid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
    Optional<PasswordResetCode> findByUserAndUsedFalse(User user);
}