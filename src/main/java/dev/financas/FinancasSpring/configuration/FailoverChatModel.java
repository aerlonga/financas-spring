package dev.financas.FinancasSpring.configuration;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Implementação de Failover para ChatLanguageModel.
 * Tenta usar o modelo primário e, em caso de erro (ex: 429 Too Many Requests no Groq),
 * alterna automaticamente para o modelo de backup (Ollama).
 */
@Slf4j
public class FailoverChatModel implements ChatLanguageModel {

    private final ChatLanguageModel primary;
    private final ChatLanguageModel backup;

    public FailoverChatModel(ChatLanguageModel primary, ChatLanguageModel backup) {
        this.primary = primary;
        this.backup = backup;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        try {
            Response<AiMessage> response = primary.generate(messages);
            log.debug("[AI-Failover] Resposta gerada pelo modelo PRIMÁRIO.");
            return response;
        } catch (Exception e) {
            log.warn("[AI-Failover] Modelo PRIMÁRIO falhou. Alternando para o backup (Ollama) devido ao erro: {}", e.getMessage());
            try {
                Response<AiMessage> response = backup.generate(messages);
                log.info("[AI-Failover] Resposta gerada pelo modelo de BACKUP (Ollama).");
                return response;
            } catch (Exception eBackup) {
                log.error("[AI-Failover] Ambos os modelos falharam. Erro backup: {}", eBackup.getMessage());
                throw eBackup;
            }
        }
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        try {
            Response<AiMessage> response = primary.generate(messages, toolSpecifications);
            log.debug("[AI-Failover] Resposta (com tools) gerada pelo modelo PRIMÁRIO.");
            return response;
        } catch (Exception e) {
            log.warn("[AI-Failover] Modelo PRIMÁRIO falhou (com tools). Alternando para o backup (Ollama) devido ao erro: {}", e.getMessage());
            try {
                Response<AiMessage> response = backup.generate(messages, toolSpecifications);
                log.info("[AI-Failover] Resposta (com tools) gerada pelo modelo de BACKUP (Ollama).");
                return response;
            } catch (Exception eBackup) {
                log.error("[AI-Failover] Ambos os modelos falharam (com tools). Erro backup: {}", eBackup.getMessage());
                throw eBackup;
            }
        }
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        try {
            Response<AiMessage> response = primary.generate(messages, toolSpecification);
            log.debug("[AI-Failover] Resposta (com tool única) gerada pelo modelo PRIMÁRIO.");
            return response;
        } catch (Exception e) {
            log.warn("[AI-Failover] Modelo PRIMÁRIO falhou (com tool única). Alternando para o backup (Ollama) devido ao erro: {}", e.getMessage());
            try {
                Response<AiMessage> response = backup.generate(messages, toolSpecification);
                log.info("[AI-Failover] Resposta (com tool única) gerada pelo modelo de BACKUP (Ollama).");
                return response;
            } catch (Exception eBackup) {
                log.error("[AI-Failover] Ambos os modelos falharam (com tool única). Erro backup: {}", eBackup.getMessage());
                throw eBackup;
            }
        }
    }
}
