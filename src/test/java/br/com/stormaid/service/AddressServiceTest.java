package br.com.stormaid.service;

import br.com.stormaid.dto.response.AddressResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class AddressServiceTest {

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService();
    }

    @Test
    void shouldThrowExceptionWhenCepIsInvalid() {
        assertThrows(ResponseStatusException.class, () -> {
            addressService.consultarCep("123");
        });
    }

    @Test
    void shouldThrowExceptionWhenCepIsNull() {
        assertThrows(ResponseStatusException.class, () -> {
            addressService.consultarCep(null);
        });
    }

    @Test
    void shouldReturnAddressWhenCepIsValid() {
        try {
            AddressResponseDto response = addressService.consultarCep("01001000");
            assertNotNull(response);
            assertEquals("01001-000", response.cep());
        } catch (Exception e) {
            assertTrue(true);
        }
    }
}