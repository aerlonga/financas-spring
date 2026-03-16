package dev.financas.FinancasSpring.bot;

import dev.financas.FinancasSpring.services.AiAssistantService;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.services.TelegramVinculoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;

@Slf4j
@Component
public class FinanceiroBot extends TelegramLongPollingBot {

    private final AiAssistantService aiService;
    private final TelegramVinculoService vinculoService;
    private final BotSessionManager sessionManager;
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
            AiAssistantService aiService,
            TelegramVinculoService vinculoService,
            BotSessionManager sessionManager,
            @Value("${bot.telegram.token}") String token) {
        super(token);
        this.aiService = aiService;
        this.vinculoService = vinculoService;
        this.sessionManager = sessionManager;
        this.webClient = WebClient.builder().build();
    }

    @Override
    public String getBotUsername() { return username; }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("[Bot] onUpdateReceived triggered. Update details: {}", update);

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            log.debug("[Bot] Update skipped - no text message present");
            return;
        }

        String chatId  = String.valueOf(update.getMessage().getChatId());
        String texto   = update.getMessage().getText();
        String nome    = update.getMessage().getFrom().getFirstName();

        log.info("[Bot] Message from chatId={}, nome={}, text='{}'", chatId, nome, texto);

        if ("!help".equalsIgnoreCase(texto.trim())) {
            log.info("[Bot] Sending help message to {}", chatId);
            enviar(chatId, buildHelpMessage());
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
            enviar(chatId, "✅ Conta deslogada com sucesso! Você pode realizar o login novamente enviando qualquer mensagem. 👋");
            return;
        }

        // Verifica se o usuário já está autenticado no sistema
        TelegramVinculo vinculo = vinculoService.obterOuCriar(chatId, nome);

        if (vinculo.getUsuario() == null) {
            // Usuário NÃO autenticado -> processa o fluxo de autenticação via chat
            processarFluxoAutenticacao(chatId, nome, texto);
            return;
        }

        try {
            String resposta = aiService.processar(chatId, nome, texto);
            enviar(chatId, resposta);

        } catch (Exception e) {
            log.error("[Bot] Erro ao processar mensagem de {}: {}", chatId, e.getMessage());
            enviar(chatId, "Ops! Tive um probleminha. Pode tentar de novo?");
        }
    }

    private void enviar(String chatId, String texto) {
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

    private String buildHelpMessage() {
        return """
            *Comandos disponíveis:* ✨
            
            📌 *Registrar gasto*
            Diga: "Gastei 50 reais no Mercado hoje em Alimentação"
            
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
            enviar(chatId, "⚠️ Use o comando assim: `/vincular SEU_CODIGO`\nO código é gerado na área de configurações do sistema web.");
            return;
        }

        try {
            String resultado = webClient.post()
                    .uri("http://localhost:" + serverPort + "/api/telegram/confirmar")
                    .bodyValue(Map.of("chatId", chatId, "codigo", codigo))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            enviar(chatId, "✅ " + resultado);
        } catch (Exception e) {
            log.error("[Bot] Falha ao vincular chatId={} codigo={}: {}", chatId, codigo, e.getMessage());
            enviar(chatId, "❌ Código inválido ou expirado. Gere um novo código em: *Configurações → Vincular Telegram*");
        }
    }

    private void processarFluxoAutenticacao(String chatId, String nome, String texto) {
        BotSessionState estado = sessionManager.getEstado(chatId);

        try {
            switch (estado) {
                case NONE -> {
                    enviar(chatId, "Olá, *" + nome + "*! 👋\nPara usar o bot, preciso identificar você.\nVocê já tem uma conta no nosso sistema?\n\nResponda *SIM* ou *NÃO*.");
                    sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_CHOICE);
                }

                case AGUARDANDO_CHOICE -> {
                    String resposta = texto.trim().toUpperCase();
                    if (resposta.equals("SIM") || resposta.equals("S")) {
                        enviar(chatId, "📧 Ótimo! Qual é o seu *e-mail* cadastrado?");
                        sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_EMAIL_LOGIN);
                    } else if (resposta.equals("NÃO") || resposta.equals("NAO") || resposta.equals("N")) {
                        enviar(chatId, "📝 Vamos criar sua conta então!\nQual é o seu *nome completo*?");
                        sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_NOME_CADASTRO);
                    } else {
                        enviar(chatId, "🤔 Por favor, responda apenas *SIM* ou *NÃO*.");
                    }
                }

                // ── FLUXO DE LOGIN ──

                case AGUARDANDO_EMAIL_LOGIN -> {
                    sessionManager.setDado(chatId, "email", texto.trim());
                    enviar(chatId, "🔑 Agora, digite sua *senha*:\n_(Dica: Pode apagar a mensagem após enviar por segurança)_");
                    sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_SENHA_LOGIN);
                }

                case AGUARDANDO_SENHA_LOGIN -> {
                    String email = sessionManager.getDado(chatId, "email");
                    String senha = texto.trim();

                    try {
                        vinculoService.autenticarEVincular(chatId, email, senha);
                        enviar(chatId, "✅ *Login realizado com sucesso!*\n\nSua conta do Telegram está vinculada ao sistema. Agora você pode registrar gastos e pedir resumos financeiros normalmente! 🚀");
                        sessionManager.limpar(chatId);
                    } catch (Exception e) {
                        enviar(chatId, "❌ E-mail ou senha incorretos.\nVamos tentar de novo. Qual é o seu *e-mail*?");
                        sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_EMAIL_LOGIN);
                    }
                }

                // ── FLUXO DE CADASTRO ──

                case AGUARDANDO_NOME_CADASTRO -> {
                    if (texto.trim().length() < 3) {
                        enviar(chatId, "⚠️ O nome deve ter pelo menos 3 caracteres. Tente novamente:");
                        return;
                    }
                    sessionManager.setDado(chatId, "nome", texto.trim());
                    enviar(chatId, "📧 Perfeito. Qual será o seu *e-mail* de acesso?");
                    sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_EMAIL_CADASTRO);
                }

                case AGUARDANDO_EMAIL_CADASTRO -> {
                    if (!texto.contains("@") || !texto.contains(".")) {
                        enviar(chatId, "⚠️ Formato de e-mail parece inválido. Tente novamente:");
                        return;
                    }
                    sessionManager.setDado(chatId, "email", texto.trim());
                    enviar(chatId, "🔑 Crie uma *senha* segura (mínimo de 6 caracteres):");
                    sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_SENHA_CADASTRO);
                }

                case AGUARDANDO_SENHA_CADASTRO -> {
                    String senha = texto.trim();
                    if (senha.length() < 6) {
                        enviar(chatId, "⚠️ A senha deve ter no mínimo 6 caracteres. Tente novamente:");
                        return;
                    }

                    String nomeCadastro = sessionManager.getDado(chatId, "nome");
                    String emailCadastro = sessionManager.getDado(chatId, "email");

                    try {
                        vinculoService.cadastrarEVincular(chatId, nome, nomeCadastro, emailCadastro, senha);
                        enviar(chatId, "✅ *Conta criada e vinculada com sucesso!*\n\nBem-vindo ao sistema financeiro, " + nome + "! Você já pode começar a gerenciar suas contas comigo. 🚀");
                        sessionManager.limpar(chatId);
                    } catch (Exception e) {
                        log.error("[Bot] Erro ao cadastrar usuario chatId={}: {}", chatId, e.getMessage());
                        
                        if (e.getMessage() != null && e.getMessage().contains("já está em uso")) {
                            enviar(chatId, "❌ Esse e-mail já está em uso.\nSe esta é sua conta, responda *SIM* para fazer login, ou digite um e-mail diferente:");
                            sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_CHOICE);
                        } else {
                            enviar(chatId, "❌ Ocorreu um erro ao criar a conta: " + e.getMessage() + "\nVamos recomeçar. Qual é o seu e-mail?");
                            sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_EMAIL_CADASTRO);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Bot] Fluxo auth bateu panic! chatId={}", chatId, e);
            enviar(chatId, "Desculpe, deu um erro técnico aqui. Vamos tentar de novo? Responda *SIM* se já tem conta ou *NÃO* para criar.");
            sessionManager.setEstado(chatId, BotSessionState.AGUARDANDO_CHOICE);
        }
    }
}
