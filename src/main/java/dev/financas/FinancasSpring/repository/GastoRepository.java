package dev.financas.FinancasSpring.repository;

import dev.financas.FinancasSpring.model.entities.Gasto;
import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    // Busca gastos por vínculo e intervalo de datas
    List<Gasto> findByTelegramVinculoAndDataGastoBetweenOrderByDataGastoDesc(
        TelegramVinculo vinculo, LocalDate inicio, LocalDate fim);

    // Busca gastos por vínculo, data e categoria
    List<Gasto> findByTelegramVinculoAndDataGastoBetweenAndCategoriaOrderByDataGastoDesc(
        TelegramVinculo vinculo, LocalDate inicio, LocalDate fim, CategoriaGasto categoria);

    // Soma total de gastos em um período
    @Query("SELECT COALESCE(SUM(g.valor), 0) FROM Gasto g WHERE g.telegramVinculo = :vinculo AND g.dataGasto BETWEEN :inicio AND :fim")
    BigDecimal somarPorPeriodo(@Param("vinculo") TelegramVinculo vinculo,
                               @Param("inicio") LocalDate inicio,
                               @Param("fim") LocalDate fim);

    // Soma por categoria em um período
    @Query("SELECT COALESCE(SUM(g.valor), 0) FROM Gasto g WHERE g.telegramVinculo = :vinculo AND g.dataGasto BETWEEN :inicio AND :fim AND g.categoria = :categoria")
    BigDecimal somarPorPeriodoECategoria(@Param("vinculo") TelegramVinculo vinculo,
                                         @Param("inicio") LocalDate inicio,
                                         @Param("fim") LocalDate fim,
                                         @Param("categoria") CategoriaGasto categoria);

    // Top 5 mais recentes para exibição no resumo
    List<Gasto> findTop5ByTelegramVinculoOrderByCriadoEmDesc(TelegramVinculo vinculo);
}
