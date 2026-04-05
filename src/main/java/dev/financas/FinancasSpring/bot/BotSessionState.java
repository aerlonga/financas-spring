package dev.financas.FinancasSpring.bot;

/**
 * Estados possíveis de um usuário durante o fluxo de autenticação no bot.
 */
public enum BotSessionState {

    /** Nenhuma autenticação em andamento (usuário já está vinculado). */
    NONE,

    /** Bot perguntou "Você já tem conta? SIM ou NÃO" — aguardando resposta. */
    AGUARDANDO_CHOICE,

    // ── Fluxo de LOGIN ──────────────────────────────────────────────────

    /** Login: aguardando o usuário digitar o e-mail. */
    AGUARDANDO_EMAIL_LOGIN,

    /** Login: e-mail recebido, aguardando a senha. */
    AGUARDANDO_SENHA_LOGIN,

    // ── Fluxo de CADASTRO ────────────────────────────────────────────────

    /** Cadastro: aguardando o nome completo. */
    AGUARDANDO_NOME_CADASTRO,

    /** Cadastro: nome recebido, aguardando o e-mail. */
    AGUARDANDO_EMAIL_CADASTRO,

    /** Cadastro: e-mail recebido, aguardando a senha. */
    AGUARDANDO_SENHA_CADASTRO,

    // ── Fluxo de Retry / Escolha após falha de login ─────────────────────

    /** Login falhou: aguardando o usuário escolher tentar novamente ou cadastrar nova conta. */
    AGUARDANDO_RETRY_OU_CADASTRO,

    // ── Fluxo de Confirmação de Comprovante ──────────────────────────────

    /** Bot leu um comprovante e exibiu os dados ao usuário — aguardando SIM ou NÃO para registrar. */
    AGUARDANDO_CONFIRMACAO_COMPROVANTE
}
