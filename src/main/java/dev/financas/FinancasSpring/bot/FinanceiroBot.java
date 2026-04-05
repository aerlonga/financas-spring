package dev.financas.FinancasSpring.bot;

import dev.financas.FinancasSpring.model.dto.TelegramMessageDTO;
import dev.financas.FinancasSpring.services.MessageQueueService;
import dev.financas.FinancasSpring.services.TelegramMediaService;
import dev.financas.FinancasSpring.services.TelegramVinculoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FinanceiroBot extends TelegramLongPollingBot {

    private final TelegramVinculoService vinculoService;
    private final BotSessionManager sessionManager;
    private final MessageQueueService messageQueueService;
    private final TelegramMediaService mediaService;
    private final WebClient webClient;

    @Value("${bot.telegram.username}")
    private String username;

    @Value("${server.port:8080}")
    private int serverPort;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("[Bot] FinanceiroBot inicializado. Username: {}, Token configurado: {}", username, getBotToken() != null ? "SIM" : "NAO");
    }

    public FinanceiroBot(
            TelegramVinculoService vinculoService,
            BotSessionManager sessionManager,
            MessageQueueService messageQueueService,
            TelegramMediaService mediaService,
            @Value("${bot.telegram.token}") String token) {
        super(token);
        this.vinculoService = vinculoService;
        this.sessionManager = sessionManager;
        this.messageQueueService = messageQueueService;
        this.mediaService = mediaService;
        this.webClient = WebClient.builder().build();
    }

    @Override
    public String getBotUsername() { return username; }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("[Bot] onUpdateReceived triggered. Update details: {}", update);

        if (!update.hasMessage()) {
            log.debug("[Bot] Update skipped - no message present");
            return;
        }

        Message msg = update.getMessage();
        String chatId = String.valueOf(msg.getChatId());
        String nome = msg.getFrom().getFirstName();

        // ── Mensagem de VOZ ──────────────────────────────────────────────
        if (msg.hasVoice()) {
            log.info("[Bot] Voice message from chatId={}", chatId);
            String fileId = msg.getVoice().getFileId();
            String mimeType = msg.getVoice().getMimeType() != null ? msg.getVoice().getMimeType() : "audio/ogg";
            handleMidia(chatId, nome, fileId, mimeType, TelegramMessageDTO.TipoMensagem.AUDIO);
            return;
        }

        // ── Mensagem de FOTO ─────────────────────────────────────────────
        if (msg.hasPhoto()) {
            log.info("[Bot] Photo message from chatId={}", chatId);
            List<PhotoSize> fotos = msg.getPhoto();
            // Pega a foto de maior resolução
            PhotoSize fotoMaior = fotos.stream()
                    .max(Comparator.comparingInt(PhotoSize::getFileSize))
                    .orElseThrow();
            handleMidia(chatId, nome, fotoMaior.getFileId(), "image/jpeg", TelegramMessageDTO.TipoMensagem.IMAGEM);
            return;
        }

        // ── Mensagem de TEXTO ────────────────────────────────────────────
        if (!msg.hasText()) {
            log.debug("[Bot] Update skipped - no text/voice/photo message present");
            return;
        }

        String texto = msg.getText();
        log.info("[Bot] Message from chatId={}, nome={}, text='{}'", chatId, nome, texto);

        if ("!help".equalsIgnoreCase(texto.trim())) {
            log.info("[Bot] Sending help message to {}", chatId);
            enviarResposta(chatId, buildHelpMessage());
            return;
        }

        // Comando /vincular CODIGO — vincula conta Telegram ao sistema
        if (texto.trim().toLowerCase().startsWith("/vincular ")) {
            String codigo = texto.trim().substring("/vincular ".length()).trim();
            handleVincular(chatId, codigo);
            return;
        }

        // Comando /deslogar
        if ("/deslogar".equalsIgnoreCase(texto.trim()) || "!deslogar".equalsIgnoreCase(texto.trim())) {
            vinculoService.desvincular(chatId);
            sessionManager.limpar(chatId);
            enviarResposta(chatId, "✅ Conta deslogada com sucesso! Você pode realizar o login novamente enviando qualquer mensagem. 👋");
            return;
        }

        // Enfileira no RabbitMQ para processamento assíncrono
        try {
            messageQueueService.submeter(chatId, nome, texto);
        } catch (Exception e) {
            log.error("[Bot] Falha ao enfileirar mensagem de chatId={}: {}", chatId, e.getMessage());
            enviarResposta(chatId, "Desculpe, estou com dificuldade para processar sua mensagem agora. Tente novamente em alguns segundos.");
        }
    }

    /**
     * Baixa um arquivo do Telegram e enfileira no RabbitMQ como mídia (áudio ou imagem).
     */
    private void handleMidia(String chatId, String nome, String fileId,
                               String mimeType, TelegramMessageDTO.TipoMensagem tipo) {
        try {
            enviarResposta(chatId, tipo == TelegramMessageDTO.TipoMensagem.AUDIO
                ? "🎤 Recebi seu áudio! Processando..."
                : "📷 Recebi sua foto! Analisando o comprovante...");

            byte[] bytes = mediaService.baixarArquivo(fileId);
            String base64 = Base64.getEncoder().encodeToString(bytes);
            messageQueueService.submeterMidia(chatId, nome, tipo, base64, mimeType);

        } catch (Exception e) {
            log.error("[Bot] Erro ao processar mídia ({}) de chatId={}: {}", tipo, chatId, e.getMessage());
            enviarResposta(chatId, "Desculpe, não consegui processar sua " +
                    (tipo == TelegramMessageDTO.TipoMensagem.AUDIO ? "mensagem de voz." : "foto.") +
                    " Tente novamente.");
        }
    }

    /**
     * Envia uma resposta ao chat do Telegram. Divide mensagens grandes em chunks.
     * Método público para ser chamado pelo TelegramMessageConsumer.
     */
    public void enviarResposta(String chatId, String texto) {
        int limite = 4000;
        if (texto.length() <= limite) {
            enviarSimples(chatId, texto);
            return;
        }
        String[] partes = texto.split("\n");
        StringBuilder chunk = new StringBuilder();
        for (String linha : partes) {
            if (chunk.length() + linha.length() > limite) {
                enviarSimples(chatId, chunk.toString());
                chunk = new StringBuilder();
            }
            chunk.append(linha).append("\n");
        }
        if (!chunk.isEmpty()) enviarSimples(chatId, chunk.toString());
    }

    private void enviarSimples(String chatId, String texto) {
        try {
            execute(SendMessage.builder()
                .chatId(chatId)
                .text(texto)
                .parseMode("Markdown")
                .build());
        } catch (TelegramApiException e) {
            try {
                execute(SendMessage.builder().chatId(chatId).text(texto).build());
            } catch (TelegramApiException ex) {
                log.error("[Bot] Falha ao enviar mensagem para {}: {}", chatId, ex.getMessage());
            }
        }
    }

    public String buildHelpMessage() {
        return """
            *Comandos disponíveis:* ✨
            
            📌 *Registrar gasto*
            Diga: "Gastei 50 reais no Mercado hoje em Alimentação"
            
            🎤 *Mensagem de voz*
            Envie um áudio dizendo o seu gasto — o bot transcreve e registra automaticamente!
            
            📷 *Comprovante / Recibo*
            Tire uma foto do comprovante — o bot lê os dados e pede confirmação antes de registrar.
            
            📊 *Consultar gastos*
            Diga: "Quanto gastei hoje?" ou "Meus gastos dessa semana"
            
            💰 *Economias*
            Diga: "Guardei 200 reais do salário"
            
            🎯 *Orçamento*
            Diga: "Qual meu orçamento de Alimentação?" ou "!orcamento"
            
            💱 *Cotações*
            Diga: "Quanto está o dólar?" ou "Preço do bitcoin"
            
            🚪 *Deslogar*
            Diga: "/deslogar"
            
            💡 *Dica:* Fale naturalmente! A IA entende português.
            """;
    }

    private void handleVincular(String chatId, String codigo) {
        if (codigo.isBlank()) {
            enviarResposta(chatId, "⚠️ Use o comando assim: `/vincular SEU_CODIGO`\nO código é gerado na área de configurações do sistema web.");
            return;
        }

        try {
            String resultado = webClient.post()
                    .uri("http://localhost:" + serverPort + "/api/telegram/confirmar")
                    .bodyValue(Map.of("chatId", chatId, "codigo", codigo))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            enviarResposta(chatId, "✅ " + resultado);
        } catch (Exception e) {
            log.error("[Bot] Falha ao vincular chatId={} codigo={}: {}", chatId, codigo, e.getMessage());
            enviarResposta(chatId, "❌ Código inválido ou expirado. Gere um novo código em: *Configurações → Vincular Telegram*");
        }
    }

    /**
     * Processa o fluxo de autenticação (login ou cadastro) via chat.
     * Chamado pelo TelegramMessageConsumer quando o usuário não está autenticado.
     */
    public void processarFluxoAutenticacao(String chatId, String nome, String texto) {
        BotSessionState estado = sessionManager.getEstado(chatId);

        try {
            switch (estado) {
                case NONE -> {
                    enviarResposta(chatId, "Olá, *" + nome + "*! 👋\nPara usar o bot, preciso identificar você.\nVocê já tem uma conta no nosso sistema?\n\nResponda *SIM* ou *NÃO*.");
                    sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_CHOICE);
                }

                case AGUARDANDO_CHOICE -> {
                    String resposta = texto.trim().toUpperCase();
                    if (resposta.equals("SIM") || resposta.equals("S")) {
                        enviarResposta(chatId, "📧 Ótimo! Qual é o seu *e-mail* cadastrado?");
                        sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_EMAIL_LOGIN);
                    } else if (resposta.equals("NÃO") || resposta.equals("NAO") || resposta.equals("N")) {
                        enviarResposta(chatId, "📝 Vamos criar sua conta então!\nQual é o seu *nome completo*?");
                        sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_NOME_CADASTRO);
                    } else {
                        enviarResposta(chatId, "🤔 Por favor, responda apenas *SIM* ou *NÃO*.");
                    }
                }

                // ── FLUXO DE LOGIN ──

                case AGUARDANDO_EMAIL_LOGIN -> {
                    sessionManager.setDado(chatId, "email", texto.trim());
                    enviarResposta(chatId, "🔑 Agora, digite sua *senha*:\n_(Dica: Pode apagar a mensagem após enviar por segurança)_");
                    sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_SENHA_LOGIN);
                }

                case AGUARDANDO_SENHA_LOGIN -> {
                    String email = sessionManager.getDado(chatId, "email");
                    String senha = texto.trim();

                    try {
                        vinculoService.autenticarEVincular(chatId, email, senha);
                        enviarResposta(chatId, "✅ *Login realizado com sucesso!*\n\nSua conta do Telegram está vinculada ao sistema. Agora você pode registrar gastos e pedir resumos financeiros normalmente! 🚀");
                        sessionManager.limpar(chatId);
                    } catch (Exception e) {
                        enviarResposta(chatId, "❌ E-mail ou senha incorretos.\nVamos tentar de novo. Qual é o seu *e-mail*?");
                        sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_EMAIL_LOGIN);
                    }
                }

                // ── FLUXO DE CADASTRO ──

                case AGUARDANDO_NOME_CADASTRO -> {
                    if (texto.trim().length() < 3) {
                        enviarResposta(chatId, "⚠️ O nome deve ter pelo menos 3 caracteres. Tente novamente:");
                        return;
                    }
                    sessionManager.setDado(chatId, "nome", texto.trim());
                    enviarResposta(chatId, "📧 Perfeito. Qual será o seu *e-mail* de acesso?");
                    sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_EMAIL_CADASTRO);
                }

                case AGUARDANDO_EMAIL_CADASTRO -> {
                    if (!texto.contains("@") || !texto.contains(".")) {
                        enviarResposta(chatId, "⚠️ Formato de e-mail parece inválido. Tente novamente:");
                        return;
                    }
                    sessionManager.setDado(chatId, "email", texto.trim());
                    enviarResposta(chatId, "🔑 Crie uma *senha* segura (mínimo de 6 caracteres):");
                    sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_SENHA_CADASTRO);
                }

                case AGUARDANDO_SENHA_CADASTRO -> {
                    String senha = texto.trim();
                    if (senha.length() < 6) {
                        enviarResposta(chatId, "⚠️ A senha deve ter no mínimo 6 caracteres. Tente novamente:");
                        return;
                    }

                    String nomeCadastro = sessionManager.getDado(chatId, "nome");
                    String emailCadastro = sessionManager.getDado(chatId, "email");

                    try {
                        vinculoService.cadastrarEVincular(chatId, nome, nomeCadastro, emailCadastro, senha);
                        enviarResposta(chatId, "✅ *Conta criada e vinculada com sucesso!*\n\nBem-vindo ao sistema financeiro, " + nome + "! Você já pode começar a gerenciar suas contas comigo. 🚀");
                        sessionManager.limpar(chatId);
                    } catch (Exception e) {
                        log.error("[Bot] Erro ao cadastrar usuario chatId={}: {}", chatId, e.getMessage());

                        if (e.getMessage() != null && e.getMessage().contains("já está em uso")) {
                            enviarResposta(chatId, "❌ Esse e-mail já está em uso.\nSe esta é sua conta, responda *SIM* para fazer login, ou digite um e-mail diferente:");
                            sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_CHOICE);
                        } else {
                            enviarResposta(chatId, "❌ Ocorreu um erro ao criar a conta: " + e.getMessage() + "\nVamos recomeçar. Qual é o seu e-mail?");
                            sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_EMAIL_CADASTRO);
                        }
                    }
                }

                // O estado AGUARDANDO_CONFIRMACAO_COMPROVANTE é tratado no TelegramMessageConsumer
                case AGUARDANDO_CONFIRMACAO_COMPROVANTE -> {
                    // Não deve cair aqui no fluxo de autenticação — o consumer cuida disso
                    log.warn("[Bot] Estado AGUARDANDO_CONFIRMACAO_COMPROVANTE alcançado no fluxo de autenticação para chatId={}", chatId);
                }
            }
        } catch (Exception e) {
            log.error("[Bot] Fluxo auth bateu panic! chatId={}", chatId, e);
            enviarResposta(chatId, "Desculpe, deu um erro técnico aqui. Vamos tentar de novo? Responda *SIM* se já tem conta ou *NÃO* para criar.");
            sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_CHOICE);
        }
    }
}
