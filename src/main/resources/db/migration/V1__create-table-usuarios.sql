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