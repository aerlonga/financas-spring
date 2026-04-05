package dev.financas.FinancasSpring.bot;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Gerencia o estado temporário da conversa de autenticação por chatId.
 * Dados ficam em memória (sem persistência) — se o servidor reiniciar o
 * usuário simplesmente recomeça o fluxo.
 * <p>
 * Sessões inativas por mais de 10 minutos são automaticamente limpas
 * para evitar memory leaks.
 */
@Slf4j
@Component
public class BotSessionManager {

    private static final long SESSION_TTL_MINUTES = 10;

    /** Estado atual de cada chatId no fluxo de auth. */
    private final Map<String, BotSessionState> estados = new ConcurrentHashMap<>();

    /** Dados parciais coletados durante o fluxo (ex: email antes de pedir a senha). */
    private final Map<String, Map<String, String>> dados = new ConcurrentHashMap<>();

    /** Timestamp da última atividade de cada chatId. */
    private final Map<String, Instant> ultimaAtividade = new ConcurrentHashMap<>();

    /** Scheduler para limpeza periódica de sessões expiradas. */
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("[BotSession] BotSessionManager inicializado. TTL de sessão: {} minutos.", SESSION_TTL_MINUTES);
        // Executa limpeza a cada 2 minutos
        cleanupScheduler.scheduleAtFixedRate(this::limparSessoesExpiradas, 2, 2, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void destroy() {
        log.info("[BotSession] Desligando cleanupScheduler...");
        cleanupScheduler.shutdownNow();
    }

    public BotSessionState getEstado(String chatId) {
        tocarSessao(chatId);
        return estados.getOrDefault(chatId, BotSessionState.NONE);
    }

    public void setEstado(String chatId, BotSessionState estado) {
        log.debug("[Session] Alterando estado de {} para {}", chatId, estado);
        estados.put(chatId, estado);
        tocarSessao(chatId);
    }

    public void setDado(String chatId, String chave, String valor) {
        log.debug("[Session] Coletando dado para {}: {} = {}", chatId, chave, (chave.contains("senha") ? "****" : valor));
        dados.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>()).put(chave, valor);
        tocarSessao(chatId);
    }

    public String getDado(String chatId, String chave) {
        Map<String, String> map = dados.get(chatId);
        return map != null ? map.get(chave) : null;
    }

    /** Limpa tudo relacionado a este chatId (após autenticação concluída ou cancelamento). */
    public void limpar(String chatId) {
        log.debug("[Session] Limpando dados da sessão de {}", chatId);
        estados.remove(chatId);
        dados.remove(chatId);
        ultimaAtividade.remove(chatId);
    }

    /** Atualiza o timestamp de última atividade. */
    private void tocarSessao(String chatId) {
        ultimaAtividade.put(chatId, Instant.now());
    }

    /** Remove sessões que não foram tocadas nos últimos SESSION_TTL_MINUTES minutos. */
    private void limparSessoesExpiradas() {
        Instant limite = Instant.now().minusSeconds(SESSION_TTL_MINUTES * 60);
        int removidas = 0;
        for (Map.Entry<String, Instant> entry : ultimaAtividade.entrySet()) {
            if (entry.getValue().isBefore(limite)) {
                String chatId = entry.getKey();
                estados.remove(chatId);
                dados.remove(chatId);
                ultimaAtividade.remove(chatId);
                removidas++;
            }
        }
        if (removidas > 0) {
            log.info("[BotSession] Limpeza automática: {} sessões expiradas removidas.", removidas);
        }
    }
}
