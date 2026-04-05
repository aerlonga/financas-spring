package dev.financas.FinancasSpring.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o estado temporário da conversa de autenticação por chatId.
 * Dados ficam em memória (sem persistência) — se o servidor reiniciar o
 * usuário simplesmente recomeça o fluxo.
 */
@Slf4j
@Component
public class BotSessionManager {

    /** Estado atual de cada chatId no fluxo de auth. */
    private final Map<String, BotSessionState> estados = new ConcurrentHashMap<>();

    /** Dados parciais coletados durante o fluxo (ex: email antes de pedir a senha). */
    private final Map<String, Map<String, String>> dados = new ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("[BotSession] BotSessionManager inicializado. Gerenciando sessões temporárias na memória...");
    }

    public BotSessionState getEstado(String chatId) {
        return estados.getOrDefault(chatId, BotSessionState.NONE);
    }

    public void setEstado(String chatId, BotSessionState estado) {
        log.debug("[Session] Alterando estado de {} para {}", chatId, estado);
        estados.put(chatId, estado);
    }

    public void setDado(String chatId, String chave, String valor) {
        log.debug("[Session] Coletando dado para {}: {} = {}", chatId, chave, (chave.contains("senha") ? "****" : valor));
        dados.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>()).put(chave, valor);
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
    }
}
