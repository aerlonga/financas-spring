package dev.financas.FinancasSpring.repository;

import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TelegramVinculoRepository extends JpaRepository<TelegramVinculo, Long> {
    Optional<TelegramVinculo> findByChatId(String chatId);
    Optional<TelegramVinculo> findByCodigoVinculo(String codigoVinculo);
}
