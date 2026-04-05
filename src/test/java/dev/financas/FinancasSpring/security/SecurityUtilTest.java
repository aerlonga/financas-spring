package dev.financas.FinancasSpring.security;

import dev.financas.FinancasSpring.model.entities.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUtilTest {

    private final SecurityUtil securityUtil = new SecurityUtil();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownerDeveAcessarProprioRecurso() {
        Usuario usuario = Usuario.builder().id(1L).email("user@email.com").role(Usuario.Role.USER).build();
        autenticar(usuario);

        assertThat(securityUtil.isOwnerOrAdmin(1L)).isTrue();
    }

    @Test
    void ownerNaoDeveAcessarRecursoDeOutro() {
        Usuario usuario = Usuario.builder().id(1L).email("user@email.com").role(Usuario.Role.USER).build();
        autenticar(usuario);

        assertThat(securityUtil.isOwnerOrAdmin(2L)).isFalse();
    }

    @Test
    void adminDeveAcessarQualquerRecurso() {
        Usuario admin = Usuario.builder().id(1L).email("admin@email.com").role(Usuario.Role.ADMIN).build();
        autenticar(admin);

        assertThat(securityUtil.isOwnerOrAdmin(99L)).isTrue();
    }

    @Test
    void usuarioNaoAutenticadoDeveRetornarFalse() {
        // Contexto vazio
        assertThat(securityUtil.isOwnerOrAdmin(1L)).isFalse();
    }

    @Test
    void principalNaoUsuarioDeveRetornarFalse() {
        // Principal é uma String em vez de Usuario
        var token = new UsernamePasswordAuthenticationToken("string-principal", null);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThat(securityUtil.isOwnerOrAdmin(1L)).isFalse();
    }

    private void autenticar(Usuario usuario) {
        var auth = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
