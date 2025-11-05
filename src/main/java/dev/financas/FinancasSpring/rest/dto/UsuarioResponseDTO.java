package dev.financas.FinancasSpring.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsuarioResponseDTO {
    private Long id;
    private String nomeCompleto;
    private String email;
    private String status;
    private String role;

    private LocalDateTime criadoEm;
    private String criadoPor;

    private LocalDateTime atualizadoEm;
    private String atualizadoPor;

    private DetalhesResponseDTO detalhes;
    private FinanceiroResponseDTO financeiro;
    private PreferenciasResponseDTO preferencias;
}
