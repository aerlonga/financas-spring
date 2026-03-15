package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.model.entities.BotMemoria;
import dev.financas.FinancasSpring.repository.BotMemoriaRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BotMemoriaService {

    private final BotMemoriaRepository repository;

    @Value("${bot.memory.max-messages:20}")
    private int maxMessages;

    /**
     * Constrói um ChatMemory com persistência no banco para um chatId específico.
     * O LangChain4j usa este objeto para manter contexto entre mensagens.
     */
    public MessageWindowChatMemory buildMemoryForChat(String chatId) {
        return MessageWindowChatMemory.builder()
            .id(chatId)
            .maxMessages(maxMessages)
            .chatMemoryStore(new PostgresChatMemoryStore(chatId, repository))
            .build();
    }

    /**
     * Implementação do ChatMemoryStore usando o repositório JPA.
     */
    private record PostgresChatMemoryStore(
        String chatId,
        BotMemoriaRepository repository
    ) implements ChatMemoryStore {

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            return repository.findTop20ByChatIdOrderByCriadoEmAsc(chatId)
                .stream()
                .map(m -> "user".equals(m.getRole())
                    ? (ChatMessage) UserMessage.from(m.getConteudo())
                    : (ChatMessage) AiMessage.from(m.getConteudo()))
                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            // Persiste apenas a última mensagem nova (não duplica)
            if (!messages.isEmpty()) {
                ChatMessage ultima = messages.get(messages.size() - 1);
                BotMemoria memoria = BotMemoria.builder()
                    .chatId(chatId)
                    .role(ultima instanceof UserMessage ? "user" : "assistant")
                    .conteudo(ultima.text())
                    .build();
                repository.save(memoria);
            }
        }

        @Override
        public void deleteMessages(Object memoryId) {
            // Implementação opcional para reset de contexto
        }
    }
}
