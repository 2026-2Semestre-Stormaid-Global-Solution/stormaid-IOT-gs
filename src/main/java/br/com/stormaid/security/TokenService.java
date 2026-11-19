package br.com.stormaid.security;

import br.com.stormaid.entity.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    private final Map<String, Instant> consumedResetTokens = new ConcurrentHashMap<>();

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("stormaid-api")
                    .withAudience("stormaid-services")
                    .withSubject(user.getEmail())
                    .withClaim("userId", user.getId())
                    .withClaim("tokenVersion", user.getTokenVersion())
                    .withClaim("roles", user.getRoles().stream().map(r -> r.getName().name()).toList())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String validateToken(String token) {
        DecodedJWT decoded = decodeSessionToken(token);
        if (decoded == null) {
            return "";
        }
        return decoded.getSubject();
    }

    public Long extractTokenVersion(String token) {
        DecodedJWT decoded = decodeSessionToken(token);
        if (decoded == null) {
            return null;
        }
        return decoded.getClaim("tokenVersion").asLong();
    }

    private DecodedJWT decodeSessionToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("stormaid-api")
                    .withAudience("stormaid-services")
                    .build()
                    .verify(token);
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    private Instant genExpirationDate() {
        return Instant.now().plusSeconds(7200);
    }

    public String generatePasswordResetToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String jti = UUID.randomUUID().toString();
            return JWT.create()
                    .withJWTId(jti)
                    .withIssuer("stormaid-api")
                    .withAudience("stormaid-password-reset")
                    .withSubject(user.getEmail())
                    .withClaim("purpose", "PASSWORD_RESET")
                    .withExpiresAt(Instant.now().plusSeconds(300))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token de redefinição de senha", exception);
        }
    }

    public String validatePasswordResetToken(String token) {
        DecodedJWT decoded = decodeAndVerifyResetToken(token);
        if (decoded == null) {
            return "";
        }
        String jti = decoded.getId();
        if (jti == null || jti.isBlank() || isTokenConsumed(jti)) {
            return "";
        }
        return decoded.getSubject();
    }

    public boolean consumePasswordResetToken(String token) {
        DecodedJWT decoded = decodeAndVerifyResetToken(token);
        if (decoded == null) {
            return false;
        }
        String jti = decoded.getId();
        if (jti == null || jti.isBlank()) {
            return false;
        }
        cleanupExpiredTokens();
        Instant expiresAt = decoded.getExpiresAtAsInstant();
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            return false;
        }
        Instant previous = consumedResetTokens.putIfAbsent(jti, expiresAt);
        return previous == null;
    }

    private DecodedJWT decodeAndVerifyResetToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("stormaid-api")
                    .withAudience("stormaid-password-reset")
                    .withClaim("purpose", "PASSWORD_RESET")
                    .build()
                    .verify(token);
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    private boolean isTokenConsumed(String jti) {
        cleanupExpiredTokens();
        return consumedResetTokens.containsKey(jti);
    }

    private void cleanupExpiredTokens() {
        Instant now = Instant.now();
        consumedResetTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}