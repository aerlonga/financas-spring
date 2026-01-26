package dev.financas.FinancasSpring.rest.dto;

import dev.financas.FinancasSpring.anottation.EmailUnicoUpdate;
import dev.financas.FinancasSpring.model.entities.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EmailUnicoUpdate
public class UsuarioUpdateDTO {

    private Long id;

    @Size(min = 5, max = 35, message = "O nome deve ter entre 5 e 35 caracteres")
    private String nomeCompleto;

    @Email(message = "E-mail inválido")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
    private String email;

    @Size(min = 6, max = 50, message = "A senha deve ter entre 6 e 50 caracteres")
    private String senha;

    private Usuario.Status status;
    private Usuario.Role role;

    @Valid
    private DetalhesUpdateDTO detalhes;
    @Valid
    private FinanceiroUpdateDTO financeiro;
    @Valid
    private PreferenciasUpdateDTO preferencias;
}
