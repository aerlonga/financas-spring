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
            List<BotMemoria> memorias = repository.findTop20ByChatIdOrderByCriadoEmDesc(chatId);
            // Inverte a lista para que as mensagens fiquem em ordem cronológica (mais antiga primeiro)
            java.util.Collections.reverse(memorias);
            
            return memorias.stream()
                .map(m -> {
                    try {
                        return dev.langchain4j.data.message.ChatMessageDeserializer.messageFromJson(m.getConteudo());
                    } catch (Exception ex) {
                        return "user".equals(m.getRole())
                            ? (ChatMessage) UserMessage.from(m.getConteudo())
                            : (ChatMessage) AiMessage.from(m.getConteudo());
                    }
                })
                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            // Persiste apenas se houver novas mensagens
            if (messages.isEmpty()) return;

            ChatMessage ultimaMsg = messages.get(messages.size() - 1);
            String conteudo;
            try {
                conteudo = dev.langchain4j.data.message.ChatMessageSerializer.messageToJson(ultimaMsg);
            } catch(Exception ex) {
                conteudo = ultimaMsg.text() != null ? ultimaMsg.text() : "{}";
            }
            
            String role = (ultimaMsg instanceof UserMessage) ? "user" : "assistant";

            // Verifica se a última mensagem no banco é igual para evitar duplicidade
            List<BotMemoria> ultimaNoBanco = repository.findTop20ByChatIdOrderByCriadoEmDesc(chatId);
            if (!ultimaNoBanco.isEmpty()) {
                BotMemoria m = ultimaNoBanco.get(0);
                if (m.getConteudo().equals(conteudo) && m.getRole().equals(role)) {
                    return; // Já cadastrado
                }
            }

            BotMemoria memoria = BotMemoria.builder()
                .chatId(chatId)
                .role(role)
                .conteudo(conteudo)
                .build();
            repository.save(memoria);
        }

        @Override
        public void deleteMessages(Object memoryId) {
            // Implementação opcional para reset de contexto
        }
    }
}
