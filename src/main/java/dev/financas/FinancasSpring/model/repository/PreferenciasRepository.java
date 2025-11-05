package dev.financas.FinancasSpring.model.repository;

import dev.financas.FinancasSpring.model.entities.Preferencias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferenciasRepository extends JpaRepository<Preferencias, Long> {
    Optional<Preferencias> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioId(Long usuarioId);
}
