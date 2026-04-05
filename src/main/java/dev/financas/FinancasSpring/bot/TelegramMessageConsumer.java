package dev.financas.FinancasSpring.bot;

import com.rabbitmq.client.Channel;
import dev.financas.FinancasSpring.configuration.RabbitMQConfig;
import dev.financas.FinancasSpring.model.dto.TelegramMessageDTO;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.services.AiAssistantService;
import dev.financas.FinancasSpring.services.GastoService;
import dev.financas.FinancasSpring.services.MessageQueueService;
import dev.financas.FinancasSpring.services.TelegramMediaService;
import dev.financas.FinancasSpring.services.TelegramVinculoService;
import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.ScheduledFuture;

/**
 * Consumidor RabbitMQ que processa as mensagens do Telegram.
 * <p>
 * Suporta três tipos de mensagem:
 * - TEXTO: fluxo normal de autenticação + IA
 * - AUDIO: transcrição via Gemini e encaminhamento para IA
 * - IMAGEM: leitura de comprovante via Gemini Vision + confirmação do usuário
 * <p>
 * Usa AcknowledgeMode.MANUAL — a mensagem só sai da fila após basicAck().
 * Em caso de erro, basicNack() envia para a DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramMessageConsumer {

    private final AiAssistantService aiService;
    private final TelegramVinculoService vinculoService;
    private final MessageQueueService messageQueueService;
    private final TelegramMediaService mediaService;
    private final GastoService gastoService;
    private final FinanceiroBot financeiroBot;
    private final BotSessionManager sessionManager;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void processarMensagem(TelegramMessageDTO mensagem,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String chatId = mensagem.getChatId();
        String nome = mensagem.getNome();
        TelegramMessageDTO.TipoMensagem tipo = mensagem.getTipo() != null
                ? mensagem.getTipo()
                : TelegramMessageDTO.TipoMensagem.TEXTO;

        // MDC para correlação de logs por chatId
        MDC.put("chatId", chatId);
        log.info("[Consumer] Processando mensagem da fila: chatId={}, tipo={}", chatId, tipo);

        try {
            switch (tipo) {
                case AUDIO -> processarAudio(chatId, nome, mensagem.getMediaBase64(), mensagem.getMimeType());
                case IMAGEM -> processarImagem(chatId, nome, mensagem.getMediaBase64(), mensagem.getMimeType());
                default -> processarTexto(chatId, nome, mensagem.getTexto());
            }

            // Mensagem processada com sucesso — confirma para o RabbitMQ
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[Consumer] Erro crítico ao processar mensagem chatId={}: {}", chatId, e.getMessage(), e);
            financeiroBot.enviarResposta(chatId, "Tive um problema para processar sua mensagem. Tente novamente em alguns segundos.");
            try {
                // Não reenfileira (requeue=false) — vai para a DLQ
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ackEx) {
                log.error("[Consumer] Falha ao fazer nack da mensagem: {}", ackEx.getMessage());
            }
        } finally {
            MDC.remove("chatId");
        }
    }

    // ── ÁUDIO ────────────────────────────────────────────────────────────────

    private void processarAudio(String chatId, String nome, String base64, String mimeType) {
        TelegramVinculo vinculo = vinculoService.obterOuCriar(chatId, nome);
        if (vinculo.getUsuario() == null) {
            financeiroBot.enviarResposta(chatId, "⚠️ Você precisa estar autenticado para enviar áudios. Por favor, envie uma mensagem de texto para iniciar o login.");
            return;
        }

        ScheduledFuture<?> typingFuture = messageQueueService.iniciarTyping(chatId, financeiroBot);
        try {
            byte[] audioBytes = Base64.getDecoder().decode(base64);
            log.info("[Consumer] Transcrevendo áudio para chatId={} ({} bytes)", chatId, audioBytes.length);

            String transcricao = mediaService.transcreverAudio(audioBytes, mimeType);
            log.info("[Consumer] Transcrição concluída para chatId={}: '{}'", chatId, transcricao);

            // Processa o texto transcrito via IA normalmente
            String respostaAi = aiService.processar(chatId, nome, "🎤 [Áudio transcrito]: " + transcricao);
            typingFuture.cancel(false);
            financeiroBot.enviarResposta(chatId, respostaAi);

        } catch (Exception e) {
            typingFuture.cancel(false);
            log.error("[Consumer] Erro na transcrição para chatId={}: {}", chatId, e.getMessage(), e);
            financeiroBot.enviarResposta(chatId, "❌ Não consegui transcrever seu áudio. Por favor, tente novamente ou escreva sua mensagem.");
        }
    }

    // ── IMAGEM ───────────────────────────────────────────────────────────────

    private void processarImagem(String chatId, String nome, String base64, String mimeType) {
        TelegramVinculo vinculo = vinculoService.obterOuCriar(chatId, nome);
        if (vinculo.getUsuario() == null) {
            financeiroBot.enviarResposta(chatId, "⚠️ Você precisa estar autenticado para enviar comprovantes. Por favor, envie uma mensagem de texto para iniciar o login.");
            return;
        }

        ScheduledFuture<?> typingFuture = messageQueueService.iniciarTyping(chatId, financeiroBot);
        try {
            byte[] imagemBytes = Base64.getDecoder().decode(base64);
            log.info("[Consumer] Lendo comprovante para chatId={} ({} bytes)", chatId, imagemBytes.length);

            String dadosExtraidos = mediaService.lerComprovante(imagemBytes, mimeType);
            log.info("[Consumer] Dados extraídos do comprovante para chatId={}: '{}'", chatId, dadosExtraidos);

            // Parseia o resultado estruturado
            DadosComprovante dados = parsearDadosComprovante(dadosExtraidos);

            if (dados.invalido()) {
                typingFuture.cancel(false);
                financeiroBot.enviarResposta(chatId,
                    "⚠️ Não consegui identificar dados financeiros nesta imagem.\n" +
                    "Por favor, envie uma foto mais nítida do comprovante ou registre o gasto manualmente.");
                return;
            }

            sessionManager.setDado(chatId, "comp_estabelecimento", dados.estabelecimento());
            sessionManager.setDado(chatId, "comp_valor", dados.valor());
            sessionManager.setDado(chatId, "comp_data", dados.data());
            sessionManager.setDado(chatId, "comp_categoria", dados.categoria());
            sessionManager.setDado(chatId, "comp_descricao", dados.descricao());
            sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_CONFIRMACAO_COMPROVANTE);

            String mascaraDesc = ("DESCONHECIDO".equalsIgnoreCase(dados.descricao()) || dados.descricao().isBlank()) ? "" : "\n• Descrição/Itens: " + dados.descricao();

            String mensagemConfirmacao = String.format(
                "📋 *Dados lidos do comprovante:*\n\n" +
                "• Local: %s\n" +
                "• Valor: R$ %s\n" +
                "• Data: %s\n" +
                "• Categoria: %s%s\n\n" +
                "As informações estão corretas? Responda *SIM* para registrar ou *NÃO* para cancelar.",
                dados.estabelecimento(),
                dados.valor(),
                dados.data(),
                dados.categoria(),
                mascaraDesc
            );

            typingFuture.cancel(false);
            financeiroBot.enviarResposta(chatId, mensagemConfirmacao);

        } catch (Exception e) {
            typingFuture.cancel(false);
            log.error("[Consumer] Erro ao ler comprovante para chatId={}: {}", chatId, e.getMessage(), e);
            financeiroBot.enviarResposta(chatId, "❌ Não consegui ler sua imagem. Por favor, tente com uma foto mais nítida ou registre o gasto manualmente.");
        }
    }

    // ── TEXTO ────────────────────────────────────────────────────────────────

    private void processarTexto(String chatId, String nome, String texto) {
        TelegramVinculo vinculo = vinculoService.obterOuCriar(chatId, nome);

        // Verifica se está no fluxo de confirmação de comprovante
        if (sessionManager.getEstado(chatId) == BotSessionState.AGUARDANDO_CONFIRMACAO_COMPROVANTE) {
            processarConfirmacaoComprovante(chatId, nome, texto, vinculo);
            return;
        }

        if (vinculo.getUsuario() == null) {
            // Usuário NÃO autenticado -> processa o fluxo de autenticação via chat
            financeiroBot.processarFluxoAutenticacao(chatId, nome, texto);
            return;
        }

        // Se o usuário já está logado e enviou uma saudação, enviamos o help para lembrá-lo das funcionalidades
        if (isSaudacao(texto)) {
            financeiroBot.enviarResposta(chatId, "Olá! 👋 Que bom te ver de novo! Aqui está um lembrete do que posso fazer por você:\n\n" + financeiroBot.buildHelpMessage());
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
    }

    private boolean isSaudacao(String texto) {
        if (texto == null || texto.isBlank()) return false;
        String t = texto.trim().toLowerCase();
        return t.equals("olá") || t.equals("ola") || t.equals("oi") || t.equals("oii") || 
               t.startsWith("bom dia") || t.startsWith("boa tarde") || t.startsWith("boa noite");
    }

    // ── CONFIRMAÇÃO DO COMPROVANTE ────────────────────────────────────────────

    private void processarConfirmacaoComprovante(String chatId, String nome, String texto, TelegramVinculo vinculo) {
        String resposta = texto.trim().toUpperCase();

        if (resposta.equals("SIM") || resposta.equals("S")) {
            try {
                String estabelecimento = sessionManager.getDado(chatId, "comp_estabelecimento");
                String valorStr = sessionManager.getDado(chatId, "comp_valor");
                String dataStr = sessionManager.getDado(chatId, "comp_data");
                String categoriaStr = sessionManager.getDado(chatId, "comp_categoria");
                String descricao = sessionManager.getDado(chatId, "comp_descricao");
                if ("DESCONHECIDO".equalsIgnoreCase(descricao)) {
                    descricao = null;
                }

                BigDecimal valor = parsearValor(valorStr);
                LocalDate data = parsearData(dataStr);
                CategoriaGasto categoria = parsearCategoria(categoriaStr);

                gastoService.registrar(vinculo, estabelecimento, valor, categoria, data, descricao);

                sessionManager.limpar(chatId);
                
                String mascaraDesc = (descricao == null || descricao.isBlank()) ? "" : "\n• Descrição: " + descricao;
                
                financeiroBot.enviarResposta(chatId, String.format(
                    "✅ *Gasto registrado com sucesso!*\n\n" +
                    "• Local: %s\n• Valor: R$ %.2f\n• Categoria: %s\n• Data: %s%s",
                    estabelecimento, valor, categoria.name(),
                    data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    mascaraDesc
                ));

            } catch (Exception e) {
                log.error("[Consumer] Erro ao registrar gasto do comprovante chatId={}: {}", chatId, e.getMessage(), e);
                sessionManager.limpar(chatId);
                financeiroBot.enviarResposta(chatId, "❌ Ocorreu um erro ao registrar o gasto. Por favor, registre manualmente.");
            }

        } else if (resposta.equals("NÃO") || resposta.equals("NAO") || resposta.equals("N")) {
            sessionManager.limpar(chatId);
            financeiroBot.enviarResposta(chatId, "❌ Registro cancelado. Você pode registrar o gasto manualmente se quiser.");

        } else {
            financeiroBot.enviarResposta(chatId, "🤔 Por favor, responda *SIM* para confirmar o registro ou *NÃO* para cancelar.");
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    /**
     * Parseia a resposta estruturada do Gemini no formato:
     * ESTABELECIMENTO: X\nVALOR: Y\nDATA: Z\nCATEGORIA: W
     */
    private DadosComprovante parsearDadosComprovante(String texto) {
        String estabelecimento = extrairCampo(texto, "ESTABELECIMENTO");
        String valor = extrairCampo(texto, "VALOR");
        String data = extrairCampo(texto, "DATA");
        String categoria = extrairCampo(texto, "CATEGORIA");
        String descricao = extrairCampo(texto, "DESCRICAO");

        return new DadosComprovante(estabelecimento, valor, data, categoria, descricao);
    }

    private String extrairCampo(String texto, String campo) {
        for (String linha : texto.split("\n")) {
            if (linha.toUpperCase().startsWith(campo + ":")) {
                return linha.substring(campo.length() + 1).trim();
            }
        }
        return "DESCONHECIDO";
    }

    private BigDecimal parsearValor(String valorStr) {
        try {
            // Remove "R$", espaços e troca vírgula por ponto
            String limpo = valorStr.replaceAll("[^0-9,.]", "").replace(",", ".");
            return new BigDecimal(limpo);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parsearData(String dataStr) {
        if (dataStr == null || dataStr.isBlank() || "DESCONHECIDO".equalsIgnoreCase(dataStr) || "hoje".equalsIgnoreCase(dataStr)) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private CategoriaGasto parsearCategoria(String categoriaStr) {
        try {
            return CategoriaGasto.valueOf(categoriaStr.toUpperCase().trim());
        } catch (Exception e) {
            return CategoriaGasto.OUTROS;
        }
    }

    /** Record imutável que representa os dados extraídos de um comprovante. */
    private record DadosComprovante(String estabelecimento, String valor, String data, String categoria, String descricao) {
        boolean invalido() {
            return "DESCONHECIDO".equalsIgnoreCase(estabelecimento) && "DESCONHECIDO".equalsIgnoreCase(valor);
        }
    }
}
