package dev.financas.FinancasSpring.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinanceiroResponseDTO {
    private Long id;
    private String profissao;
    private BigDecimal rendaMensal;
    private String tipoRenda;
    private String objetivoFinanceiro;
    private BigDecimal metaPoupancaMensal;
}