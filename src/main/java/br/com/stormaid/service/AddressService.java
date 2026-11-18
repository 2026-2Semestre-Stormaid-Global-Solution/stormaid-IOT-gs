package br.com.stormaid.service;

import br.com.stormaid.dto.response.AddressResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AddressService {

    private final RestTemplate restTemplate = new RestTemplate();

    public AddressResponseDto consultarCep(String cep) {
        if (cep == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O CEP não pode ser nulo.");
        }

        String cepLimpo = cep.replaceAll("\\D", "");

        if (cepLimpo.length() != 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CEP inválido. Deve conter 8 dígitos.");
        }

        String url = "https://viacep.com.br/ws/" + cepLimpo + "/json/";

        try {
            AddressResponseDto response = restTemplate.getForObject(url, AddressResponseDto.class);

            if (response == null || response.cep() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CEP não encontrado.");
            }

            return response;
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erro ao consultar o CEP na origem externa.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço ViaCEP temporariamente indisponível.");
        }
    }
}