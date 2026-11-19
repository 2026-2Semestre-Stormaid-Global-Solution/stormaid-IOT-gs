package br.com.stormaid.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public class RegisterRequest {

    @NotBlank(message = "Full name is mandatory")
    @Size(min = 3, max = 150, message = "Full name must be between 3 and 150 characters")
    private String fullName;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Phone number is mandatory")
    @Size(max = 20)
    private String phoneNumber;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 6, max = 255, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "CPF is mandatory")
    @CPF(message = "Invalid CPF")
    private String cpf;

    @NotNull(message = "Birth date is mandatory")
    @PastOrPresent(message = "Birth date cannot be in the future")
    private LocalDate birthDate;

    // Construtores
    public RegisterRequest() {}

    public RegisterRequest(String fullName, String email, String phoneNumber, String password, String cpf, LocalDate birthDate) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.cpf = cpf;
        this.birthDate = birthDate;
    }

    // Getters e Setters explícitos
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}
