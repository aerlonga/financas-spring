package dev.financas.FinancasSpring.rest.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceiroCreateDTO {

    @Size(max = 50, message = "A profissão deve ter no máximo 50 caracteres.")
    private String profissao;

    @PositiveOrZero(message = "A renda mensal deve ser um valor positivo.")
    @Digits(integer = 8, fraction = 2, message = "Formato de renda mensal inválido (máximo 8 dígitos inteiros e 2 decimais).")
    private BigDecimal rendaMensal;

    @Size(max = 50, message = "O tipo de renda deve ter no máximo 50 caracteres.")
    private String tipoRenda;

    @Size(max = 255, message = "O objetivo financeiro deve ter no máximo 255 caracteres.")
    private String objetivoFinanceiro;

    @PositiveOrZero(message = "A meta de poupança deve ser um valor positivo.")
    @Digits(integer = 8, fraction = 2, message = "Formato de meta de poupança inválido (máximo 8 dígitos inteiros e 2 decimais).")
    private BigDecimal metaPoupancaMensal;
}