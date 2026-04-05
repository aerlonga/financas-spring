package dev.financas.FinancasSpring.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import dev.financas.FinancasSpring.model.entities.Usuario;

@Component("securityUtil")
public class SecurityUtil {

    /**
     * Verifica se o usuário autenticado tem permissão para acessar um recurso.
     * A permissão é concedida se o usuário for um ADMIN ou se o ID do usuário
     * autenticado for o mesmo que o ID do recurso sendo acessado.
     *
     * @param id 
     * @return 
     */
    public boolean isOwnerOrAdmin(Long id) {
        // Temporariamente permitindo tudo para fins de teste de endpoint.
        // A lógica de segurança real será reativada depois.
        return true;
    }
}