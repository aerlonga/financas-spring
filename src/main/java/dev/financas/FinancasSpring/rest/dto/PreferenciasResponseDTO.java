package dev.financas.FinancasSpring.rest.dto;

import dev.financas.FinancasSpring.model.entities.Preferencias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PreferenciasResponseDTO {
    private Long id;
    private Preferencias.TemaInterface temaInterface;
    private Boolean notificacoesAtivadas;
    private String moedaPreferida;
    private String avatarUrl;
}