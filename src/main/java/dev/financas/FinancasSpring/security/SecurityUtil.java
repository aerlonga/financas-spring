package dev.financas.FinancasSpring.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import dev.financas.FinancasSpring.model.entities.Usuario;

@Component("securityUtil")
public class SecurityUtil {

    /**
     * Verifica se o usuário autenticado tem permissão para acessar um recurso.
     * A permissão é concedida se:
     * - O usuário for um ADMIN, ou
     * - O ID do usuário autenticado for o mesmo que o ID do recurso sendo acessado.
     *
     * @param id ID do recurso/usuário sendo acessado
     * @return true se o acesso for permitido
     */
    public boolean isOwnerOrAdmin(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Usuario usuario)) {
            return false;
        }

        // Admin tem acesso a tudo
        if (usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        // Owner: o ID do usuário autenticado deve ser igual ao ID do recurso
        return usuario.getId() != null && usuario.getId().equals(id);
    }
}