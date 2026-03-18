package dev.financas.FinancasSpring.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.bots.AbsSender;

import jakarta.annotation.PreDestroy;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Serviço de fila de mensagens por chatId.
 * <p>
 * Garante:
 * - Mensagens do MESMO chat são processadas em ORDEM (FIFO)
 * - Mensagens de chats DIFERENTES rodam em paralelo
 * - Typing indicator ("digitando...") é mostrado enquanto processa
 */
@Slf4j
@Service
public class MessageQueueService {

    /** Thread pool para processar mensagens em paralelo entre chats. */
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    /** Scheduler dedicado para enviar o typing indicator periodicamente. */
    private final ScheduledExecutorService typingScheduler = Executors.newScheduledThreadPool(2);

    /** Fila de tarefas pendentes por chatId. */
    private final ConcurrentHashMap<String, Queue<Runnable>> filas = new ConcurrentHashMap<>();

    /** Flag de "processando" por chatId — garante que apenas 1 tarefa do mesmo chat roda por vez. */
    private final ConcurrentHashMap<String, AtomicBoolean> processando = new ConcurrentHashMap<>();

    /**
     * Enfileira uma tarefa para processamento.
     * Se o chat já está sendo processado, a tarefa fica na fila.
     * Se não, executa imediatamente no thread pool.
     */
    public void submeter(String chatId, Runnable tarefa) {
        filas.computeIfAbsent(chatId, k -> new ConcurrentLinkedQueue<>()).add(tarefa);
        processando.computeIfAbsent(chatId, k -> new AtomicBoolean(false));

        tentarProcessar(chatId);
    }

    private void tentarProcessar(String chatId) {
        AtomicBoolean flag = processando.get(chatId);
        Queue<Runnable> fila = filas.get(chatId);

        if (flag == null || fila == null) return;

        // compareAndSet garante que só uma thread por chat entra aqui
        if (flag.compareAndSet(false, true)) {
            Runnable proxima = fila.poll();
            if (proxima != null) {
                executor.submit(() -> {
                    try {
                        proxima.run();
                    } catch (Exception e) {
                        log.error("[Queue] Erro ao processar tarefa para chatId={}: {}", chatId, e.getMessage(), e);
                    } finally {
                        flag.set(false);
                        // Verifica se há mais mensagens na fila
                        if (!fila.isEmpty()) {
                            tentarProcessar(chatId);
                        }
                    }
                });
            } else {
                flag.set(false);
            }
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

    @PreDestroy
    public void shutdown() {
        log.info("[Queue] Desligando thread pools...");
        executor.shutdown();
        typingScheduler.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!typingScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                typingScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            typingScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("[Queue] Thread pools desligados.");
    }
}
