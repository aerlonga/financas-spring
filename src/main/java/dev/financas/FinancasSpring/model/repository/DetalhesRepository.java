package dev.financas.FinancasSpring.model.repository;

import dev.financas.FinancasSpring.model.entities.Detalhes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DetalhesRepository extends JpaRepository<Detalhes, Long> {
    Optional<Detalhes> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    Optional<Detalhes> findByUsuarioId(Long usuarioId);
}