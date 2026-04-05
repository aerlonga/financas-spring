package dev.financas.FinancasSpring.repository;

import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.OrcamentoCategoria;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<OrcamentoCategoria, Long> {
    List<OrcamentoCategoria> findByTelegramVinculo(TelegramVinculo vinculo);
    Optional<OrcamentoCategoria> findByTelegramVinculoAndCategoria(TelegramVinculo vinculo, CategoriaGasto categoria);
}
