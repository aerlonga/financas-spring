package dev.financas.FinancasSpring.repository;

import dev.financas.FinancasSpring.model.entities.BotMemoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface BotMemoriaRepository extends JpaRepository<BotMemoria, Long> {

    // Retorna as últimas N mensagens de um chat (para reconstruir contexto)
    List<BotMemoria> findTop20ByChatIdOrderByCriadoEmDesc(String chatId);

    // Para o scheduler de limpeza
    @Modifying
    @Query("DELETE FROM BotMemoria m WHERE m.criadoEm < :limite")
    void deleteBycriadoEmBefore(@Param("limite") LocalDateTime limite);
}
