package dev.financas.FinancasSpring.services;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.financas.FinancasSpring.bot.tools.FinanceiroTools;
import dev.financas.FinancasSpring.bot.tools.PesquisaWebTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class AiAssistantService {

    private final TelegramVinculoService vinculoService;
    private AssistentFinanceiro assistente; // Agente reutilizável

    interface AssistentFinanceiro {
        @SystemMessage("{{systemPrompt}}")
        String chat(@MemoryId String chatId, @UserMessage String mensagem);
    }

    public AiAssistantService(
            ChatLanguageModel geminiChatModel,
            BotMemoriaService memoriaService,
            FinanceiroTools financeiroTools,
            PesquisaWebTools pesquisaWebTools,
            TelegramVinculoService vinculoService,
            @Value("${bot.ai.system-prompt}") String systemPrompt) {
        
        this.vinculoService = vinculoService;

        this.assistente = AiServices.builder(AssistentFinanceiro.class)
                .chatLanguageModel(geminiChatModel)
                .chatMemoryProvider(chatId -> memoriaService.buildMemoryForChat((String) chatId))
                .tools(financeiroTools, pesquisaWebTools)
                .systemMessageProvider(chatId -> systemPrompt)
                .build();
    }

    public String processar(String chatId, String pushname, String mensagem) {
        try {
            vinculoService.obterOuCriar(chatId, pushname);

            String resposta = assistente.chat(chatId, mensagem);
            
            log.info("[AI] chatId={} | resposta gerada com sucesso", chatId);
            return resposta;

        } catch (Exception e) {
            log.error("[AI] Erro ao processar mensagem chatId={}: {}", chatId, e.getMessage(), e);
            return "Tive um problema para processar sua mensagem. Tente novamente em alguns segundos.";
        }
    }
}