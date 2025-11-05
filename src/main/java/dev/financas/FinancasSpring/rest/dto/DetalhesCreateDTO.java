package dev.financas.FinancasSpring.rest.dto;

import dev.financas.FinancasSpring.anottation.CpfUnico;
import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.Past;
// import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DetalhesCreateDTO {

    private LocalDate dataNascimento;

    @Size(max = 20)
    private String genero;

    @Size(max = 20)
    private String telefone;

    @CpfUnico
    @NotBlank
    @Size(min = 11, max = 14)
    private String cpf;

    @Size(max = 9)
    private String cep;

    @Size(max = 255)
    private String endereco;

    @Size(max = 10)
    private String numero;

    @Size(max = 100)
    private String bairro;

    @Size(max = 100)
    private String cidade;

    @Size(max = 2)
    private String estado;
}