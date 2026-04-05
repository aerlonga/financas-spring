package dev.financas.FinancasSpring.repository;

import dev.financas.FinancasSpring.model.entities.Economia;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface EconomiaRepository extends JpaRepository<Economia, Long> {

    @Query("SELECT COALESCE(SUM(e.valor), 0) FROM Economia e WHERE e.telegramVinculo = :vinculo AND e.dataEconomia BETWEEN :inicio AND :fim")
    BigDecimal somarPorPeriodo(@Param("vinculo") TelegramVinculo vinculo,
                               @Param("inicio") LocalDate inicio,
                               @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(e.valor), 0) FROM Economia e WHERE e.telegramVinculo = :vinculo")
    BigDecimal somarTotal(@Param("vinculo") TelegramVinculo vinculo);
}
