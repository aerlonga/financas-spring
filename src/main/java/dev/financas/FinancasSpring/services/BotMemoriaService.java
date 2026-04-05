package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.model.entities.BotMemoria;
import dev.financas.FinancasSpring.repository.BotMemoriaRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.memory.ChatMemory;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotMemoriaService {

    private final BotMemoriaRepository repository;

    @Value("${bot.memory.max-messages:20}")
    private int maxMessages;

    public ChatMemory buildMemoryForChat(String chatId) {
        return new SafeChatMemory(chatId, new PostgresChatMemoryStore(chatId, repository), maxMessages);
    }

    public static class SafeChatMemory implements ChatMemory {
        private final Object id;
        private final ChatMemoryStore store;
        private final int maxMessages;
        private final List<ChatMessage> messages;

        public SafeChatMemory(Object id, ChatMemoryStore store, int maxMessages) {
            this.id = id;
            this.store = store;
            this.maxMessages = maxMessages;
            this.messages = new ArrayList<>();
            List<ChatMessage> fromStore = store.getMessages(id);
            if (fromStore != null) {
                this.messages.addAll(fromStore);
            }
            evict();
        }

        @Override
        public Object id() { return id; }

        @Override
        public void add(ChatMessage message) {
            messages.add(message);
            evict();
            store.updateMessages(id, this.messages);
        }

        @Override
        public List<ChatMessage> messages() {
            return new ArrayList<>(messages);
        }

        @Override
        public void clear() {
            messages.clear();
            store.deleteMessages(id);
        }

        private void evict() {
            if (messages.size() <= maxMessages) return;

            int toDrop = messages.size() - maxMessages;
            int firstUser = -1;

            for (int i = toDrop; i < messages.size(); i++) {
                if (messages.get(i).type() == ChatMessageType.USER || messages.get(i).type() == ChatMessageType.SYSTEM) {
                    firstUser = i;
                    break;
                }
            }

            if (firstUser != -1) {
                toDrop = firstUser;
            } else {
                if (!messages.isEmpty() && (messages.get(messages.size() - 1).type() == ChatMessageType.USER || messages.get(messages.size() - 1).type() == ChatMessageType.SYSTEM)) {
                    toDrop = messages.size() - 1;
                } else {
                    toDrop = messages.size();
                }
            }
            
            if (toDrop > 0 && toDrop <= messages.size()) {
                messages.subList(0, toDrop).clear();
            }
        }
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
            
            List<ChatMessage> mensagens = memorias.stream()
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

            return sanitizar(mensagens);
        }

        /**
         * Remove sequências inválidas do histórico que causam HTTP 400 na API do Gemini:
         * - AiMessage com tool calls sem ToolExecutionResultMessage correspondente logo após.
         * - ToolExecutionResultMessage sem AiMessage com tool calls imediatamente antes.
         */
        private List<ChatMessage> sanitizar(List<ChatMessage> mensagens) {
            List<ChatMessage> resultado = new ArrayList<>(mensagens);
            boolean modificado;
            
            do {
                modificado = false;

                // Garante que a primeira mensagem seja do tipo USER ou SYSTEM 
                // para evitar erros 400 do Gemini
                while (!resultado.isEmpty()) {
                    ChatMessage msg = resultado.get(0);
                    if (msg.type() != ChatMessageType.USER && msg.type() != ChatMessageType.SYSTEM) {
                        log.warn("[BotMemoria] Removendo mensagem inicial sem USER precedente: {} (chatId={})", msg.type(), chatId);
                        resultado.remove(0);
                        modificado = true;
                    } else {
                        break;
                    }
                }

                if (resultado.isEmpty()) break;
                
                // 1. Remove ToolExecutionResultMessage órfãos
                for (int i = 0; i < resultado.size(); i++) {
                    ChatMessage msg = resultado.get(i);
                    if (msg.type() == ChatMessageType.TOOL_EXECUTION_RESULT) {
                        dev.langchain4j.data.message.ToolExecutionResultMessage toolMsg = 
                            (dev.langchain4j.data.message.ToolExecutionResultMessage) msg;
                        boolean encontrouAi = false;
                        for (int j = i - 1; j >= 0; j--) {
                            ChatMessage prev = resultado.get(j);
                            if (prev.type() == ChatMessageType.AI) {
                                AiMessage ai = (AiMessage) prev;
                                if (ai.hasToolExecutionRequests() && 
                                    ai.toolExecutionRequests().stream()
                                        .anyMatch(req -> java.util.Objects.equals(req.id(), toolMsg.id()))) {
                                    encontrouAi = true;
                                }
                                break; 
                            }
                        }
                        if (!encontrouAi) {
                            log.warn("[BotMemoria] Removendo ToolExecutionResultMessage órfão: {} (chatId={})", toolMsg.id(), chatId);
                            resultado.remove(i);
                            modificado = true;
                            break;
                        }
                    }
                }
                
                if (modificado) continue;
                
                // 2. Remove AiMessages com tool requests incompletos
                for (int i = 0; i < resultado.size(); i++) {
                    ChatMessage msg = resultado.get(i);
                    if (msg.type() == ChatMessageType.AI) {
                        AiMessage aiMsg = (AiMessage) msg;
                        if (aiMsg.hasToolExecutionRequests()) {
                            boolean todasRespondidas = true;
                            for (dev.langchain4j.agent.tool.ToolExecutionRequest req : aiMsg.toolExecutionRequests()) {
                                boolean encontrouResposta = false;
                                for (int j = i + 1; j < resultado.size(); j++) {
                                    ChatMessage nextMsg = resultado.get(j);
                                    if (nextMsg.type() == ChatMessageType.TOOL_EXECUTION_RESULT) {
                                        dev.langchain4j.data.message.ToolExecutionResultMessage toolResp = 
                                            (dev.langchain4j.data.message.ToolExecutionResultMessage) nextMsg;
                                        if (java.util.Objects.equals(toolResp.id(), req.id())) {
                                            encontrouResposta = true;
                                            break;
                                        }
                                    } else if (nextMsg.type() == ChatMessageType.USER || nextMsg.type() == ChatMessageType.AI) {
                                        break; 
                                    }
                                }
                                if (!encontrouResposta) {
                                    todasRespondidas = false;
                                    break;
                                }
                            }
                            
                            boolean hasNextResult = (i + 1 < resultado.size() && resultado.get(i + 1).type() == ChatMessageType.TOOL_EXECUTION_RESULT);

                            if (!todasRespondidas || !hasNextResult) {
                                log.warn("[BotMemoria] Removendo AiMessage com tool calls incompletas na posição {} (chatId={})", i, chatId);
                                resultado.remove(i);
                                modificado = true;
                                break;
                            }
                        }
                    }
                }
                
            } while (modificado);

            return resultado;
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
