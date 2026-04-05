package dev.financas.FinancasSpring.tasks;

import dev.financas.FinancasSpring.repository.BotMemoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoriaCleanupTask {

    private final BotMemoriaRepository repository;

    @Value("${bot.memory.ttl-days:30}")
    private int ttlDias;

    // Executa todo dia à meia-noite
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void limparMemoriasAntigas() {
        LocalDateTime limite = LocalDateTime.now().minusDays(ttlDias);
        repository.deleteBycriadoEmBefore(limite);
        log.info("[Scheduler] Limpeza de memórias anteriores a {} concluída.", limite);
    }
}
