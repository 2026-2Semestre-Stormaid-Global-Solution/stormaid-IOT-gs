package br.com.stormaid.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSerializationTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldNotExposePasswordInJsonResponse() throws Exception {
        String passwordHash = "$2a$10$exampleHashForSerializationTest";

        User user = User.builder()
                .id(1L)
                .fullName("Usuário Teste")
                .email("teste@stormaid.com")
                .password(passwordHash)
                .build();

        String json = objectMapper.writeValueAsString(user);

        assertTrue(json.contains("teste@stormaid.com"));
        assertFalse(json.contains("\"password\""));
        assertFalse(json.contains(passwordHash));
        assertEquals(passwordHash, user.getPassword());
    }
}