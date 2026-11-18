package br.com.stormaid.controller;

import br.com.stormaid.dto.response.AddressResponseDto;
import br.com.stormaid.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/addresses")
@Tag(name = "Addresses", description = "Endpoints de consulta de endereços e CEP")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/cep/{cep}")
    @Operation(summary = "Consultar endereço por CEP", description = "Realiza a consulta externa do endereço utilizando o ViaCEP sem persistir dados.")
    @ApiResponse(responseCode = "200", description = "CEP encontrado com sucesso")
    @ApiResponse(responseCode = "400", description = "CEP com formato inválido")
    @ApiResponse(responseCode = "404", description = "CEP não encontrado")
    @ApiResponse(responseCode = "503", description = "Serviço externo indisponível")
    public ResponseEntity<AddressResponseDto> consultarCep(@PathVariable String cep) {
        AddressResponseDto endereco = addressService.consultarCep(cep);
        return ResponseEntity.ok(endereco);
    }
}