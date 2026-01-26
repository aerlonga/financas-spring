package dev.financas.FinancasSpring.model.repository;

import dev.financas.FinancasSpring.model.entities.Financeiro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinanceiroRepository extends JpaRepository<Financeiro, Long> {
    Optional<Financeiro> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioId(Long usuarioId);
}