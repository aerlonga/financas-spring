package dev.financas.FinancasSpring.bot;

import dev.financas.FinancasSpring.configuration.RabbitMQConfig;
import dev.financas.FinancasSpring.model.dto.TelegramMessageDTO;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.services.AiAssistantService;
import dev.financas.FinancasSpring.services.MessageQueueService;
import dev.financas.FinancasSpring.services.TelegramVinculoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

/**
 * Consumidor RabbitMQ que processa as mensagens do Telegram.
 * <p>
 * Este componente escuta a fila {@code telegram.messages} e realiza:
 * - Verificação de autenticação do usuário
 * - Processamento da IA para usuários autenticados
 * - Fluxo de autenticação para novos usuários
 * - Envio de respostas de volta ao Telegram via instância do bot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramMessageConsumer {

    private final AiAssistantService aiService;
    private final TelegramVinculoService vinculoService;
    private final MessageQueueService messageQueueService;
    private final FinanceiroBot financeiroBot;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void processarMensagem(TelegramMessageDTO mensagem) {
        String chatId = mensagem.getChatId();
        String nome = mensagem.getNome();
        String texto = mensagem.getTexto();

        log.info("[Consumer] Processando mensagem da fila: chatId={}, texto='{}'", chatId, texto);

        try {
            // Verifica se o usuário já está autenticado no sistema
            TelegramVinculo vinculo = vinculoService.obterOuCriar(chatId, nome);

            if (vinculo.getUsuario() == null) {
                // Usuário NÃO autenticado -> processa o fluxo de autenticação via chat
                financeiroBot.processarFluxoAutenticacao(chatId, nome, texto);
                return;
            }

            // Inicia typing indicator ("digitando..." no Telegram)
            ScheduledFuture<?> typingFuture = messageQueueService.iniciarTyping(chatId, financeiroBot);

            try {
                String resposta = aiService.processar(chatId, nome, texto);
                typingFuture.cancel(false);
                financeiroBot.enviarResposta(chatId, resposta);

            } catch (Exception e) {
                typingFuture.cancel(false);
                log.error("[Consumer] Erro ao processar IA para chatId={}: {}", chatId, e.getMessage(), e);
                financeiroBot.enviarResposta(chatId, "Ops! Tive um probleminha. Pode tentar de novo?");
            }

        } catch (Exception e) {
            log.error("[Consumer] Erro crítico ao processar mensagem chatId={}: {}", chatId, e.getMessage(), e);
            financeiroBot.enviarResposta(chatId, "Tive um problema para processar sua mensagem. Tente novamente em alguns segundos.");
            // Não relança a exceção para evitar requeue infinito — a mensagem vai para DLQ se necessário
        }
    }
}
