-- Habilita a extensão para tipos de texto case-insensitive
CREATE EXTENSION IF NOT EXISTS citext;

-- Cria a tabela de usuários
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome_completo VARCHAR(255) NOT NULL,
    -- Usando CITEXT para o e-mail, garantindo unicidade case-insensitive
    email CITEXT NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ATIVO',
    role VARCHAR(10) NOT NULL DEFAULT 'USER',
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    criado_por VARCHAR(255),
    atualizado_por VARCHAR(255)
);

-- Tabela para gerenciar vínculos do Telegram
CREATE TABLE telegram_vinculos (
    id BIGSERIAL PRIMARY KEY,
    chat_id VARCHAR(50) NOT NULL UNIQUE,
    pushname VARCHAR(100),
    vinculado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_atividade TIMESTAMP,
    usuario_id BIGINT REFERENCES usuarios(id)
);

CREATE INDEX idx_telegram_chat_id ON telegram_vinculos(chat_id);