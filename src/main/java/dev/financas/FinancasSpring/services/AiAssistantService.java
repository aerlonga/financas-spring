package dev.financas.FinancasSpring.services;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.data.document.Metadata;
import dev.financas.FinancasSpring.bot.tools.FinanceiroTools;
import dev.financas.FinancasSpring.bot.tools.PesquisaWebTools;
import lombok.extern.slf4j.Slf4j;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiAssistantService {
    
    private final TelegramVinculoService vinculoService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final AssistentFinanceiro assistente;

    interface AssistentFinanceiro {
        String chat(@MemoryId String chatId, @UserMessage String mensagem);
    }

    public AiAssistantService(
            // ChatLanguageModel geminiChatModel,
            ChatLanguageModel chatLanguageModel,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            BotMemoriaService memoriaService,
            FinanceiroTools financeiroTools,
            PesquisaWebTools pesquisaWebTools,
            TelegramVinculoService vinculoService,
            @Value("${bot.ai.system-prompt}") String systemPrompt) {
        
        this.vinculoService = vinculoService;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;

        ContentRetriever retriever = query -> {
            String chatId = FinanceiroTools.getChatId();
            if (chatId == null) return Collections.emptyList();

            Filter filter = MetadataFilterBuilder.metadataKey("chatId").isEqualTo(chatId);

            return EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .filter(filter)
                    .maxResults(2)
                    .minScore(0.6)
                    .build()
                    .retrieve(query);
        };

        this.assistente = AiServices.builder(AssistentFinanceiro.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatId -> memoriaService.buildMemoryForChat((String) chatId))
                .contentRetriever(retriever)
                .tools(financeiroTools, pesquisaWebTools)
                .systemMessageProvider(chatId -> systemPrompt)
                .build();
    }

    public String processar(String chatId, String pushname, String mensagem) {
        try {
            vinculoService.obterOuCriar(chatId, pushname);
            if (mensagem != null && mensagem.length() > 5) {
                TextSegment segment = TextSegment.from(mensagem, Metadata.from("chatId", chatId));
                embeddingStore.add(embeddingModel.embed(segment).content(), segment);
            }

            FinanceiroTools.setChatId(chatId);
            String resposta = assistente.chat(chatId, mensagem);
            
            log.info("[AI] chatId={} | resposta gerada com sucesso", chatId);
            return resposta;

        } catch (Exception e) {
            log.error("[AI] Erro ao processar mensagem chatId={}: {}", chatId, e.getMessage(), e);
            return "Tive um problema para processar sua mensagem. Tente novamente em alguns segundos.";
        } finally {
            FinanceiroTools.clearChatId();
        }
    }
}