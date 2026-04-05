package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.configuration.RabbitMQConfig;
import dev.financas.FinancasSpring.model.dto.TelegramMessageDTO;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Serviço de fila de mensagens usando RabbitMQ.
 * <p>
 * Responsabilidades:
 * - Publicar mensagens na fila do RabbitMQ
 * - Gerenciar o typing indicator ("digitando...") no Telegram
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueueService {

    private final RabbitTemplate rabbitTemplate;

    /** Scheduler dedicado para enviar o typing indicator periodicamente. */
    private final ScheduledExecutorService typingScheduler = Executors.newScheduledThreadPool(2);

    @PreDestroy
    public void destroy() {
        log.info("[Queue] Desligando typingScheduler...");
        typingScheduler.shutdownNow();
    }

    /**
     * Publica uma mensagem na fila do RabbitMQ para processamento assíncrono.
     */
    public void submeter(String chatId, String nome, String texto) {
        TelegramMessageDTO mensagem = new TelegramMessageDTO(chatId, nome, texto);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    mensagem
            );
            log.info("[Queue] Mensagem enfileirada no RabbitMQ para chatId={}", chatId);
        } catch (Exception e) {
            log.error("[Queue] Falha ao enfileirar mensagem para chatId={}: {}", chatId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Publica uma mensagem de mídia (áudio ou imagem) na fila do RabbitMQ.
     *
     * @param chatId     ID do chat Telegram
     * @param nome       nome do usuário
     * @param tipo       tipo de mídia (AUDIO ou IMAGEM)
     * @param mediaBase64 conteúdo do arquivo codificado em Base64
     * @param mimeType   MIME type do arquivo
     */
    public void submeterMidia(String chatId, String nome,
                               TelegramMessageDTO.TipoMensagem tipo,
                               String mediaBase64, String mimeType) {
        TelegramMessageDTO mensagem = new TelegramMessageDTO(chatId, nome, tipo, mediaBase64, mimeType);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    mensagem
            );
            log.info("[Queue] Mídia ({}) enfileirada no RabbitMQ para chatId={}", tipo, chatId);
        } catch (Exception e) {
            log.error("[Queue] Falha ao enfileirar mídia para chatId={}: {}", chatId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Inicia o loop de typing indicator.
     * Envia "typing..." a cada 4 segundos até que o retorno (ScheduledFuture) seja cancelado.
     *
     * @param chatId   ID do chat Telegram
     * @param bot      instância do bot para executar a action
     * @return ScheduledFuture que deve ser cancelado quando a resposta estiver pronta
     */
    public ScheduledFuture<?> iniciarTyping(String chatId, AbsSender bot) {
        // Envia imediatamente a primeira vez
        enviarChatAction(chatId, bot);

        // Repete a cada 4 segundos (Telegram expira typing em ~5s)
        return typingScheduler.scheduleAtFixedRate(
                () -> enviarChatAction(chatId, bot),
                4, 4, TimeUnit.SECONDS
        );
    }

    private void enviarChatAction(String chatId, AbsSender bot) {
        try {
            bot.execute(SendChatAction.builder()
                    .chatId(chatId)
                    .action(ActionType.TYPING.toString())
                    .build());
        } catch (Exception e) {
            log.warn("[Typing] Falha ao enviar typing para chatId={}: {}", chatId, e.getMessage());
        }
    }
}
