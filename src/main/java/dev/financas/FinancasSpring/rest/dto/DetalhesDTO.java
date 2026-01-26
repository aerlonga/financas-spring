package dev.financas.FinancasSpring.rest.dto;

import dev.financas.FinancasSpring.anottation.CpfUnico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DetalhesDTO {

    @Past(message = "A data de nascimento deve ser no passado.")
    private LocalDate dataNascimento;

    @Size(max = 20, message = "O gênero deve ter no máximo 20 caracteres.")
    private String genero;

    @Size(min = 10, max = 15, message = "O telefone deve ter entre 10 e 15 caracteres.")
    private String telefone;

    @CpfUnico
    @NotBlank(message = "CPF é obrigatório.")
    @Pattern(regexp = "(^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$)|(^\\d{11}$)", message = "Formato de CPF inválido.")
    private String cpf;

    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "Formato de CEP inválido.")
    @Size(max = 9, message = "O CEP deve ter no máximo 9 caracteres.")
    private String cep;

    @Size(max = 255, message = "O endereço deve ter no máximo 255 caracteres.")
    private String endereco;

    @Size(max = 10, message = "O número deve ter no máximo 10 caracteres.")
    private String numero;

    @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres.")
    private String bairro;

    @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres.")
    private String cidade;

    @Size(max = 2, message = "O estado deve ter no máximo 2 caracteres.")
    private String estado;
}