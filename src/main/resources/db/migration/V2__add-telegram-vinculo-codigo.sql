-- Cria a tabela caso ela não tenha sido criada no V1 (devido a baseline)
CREATE TABLE IF NOT EXISTS telegram_vinculos (
    id BIGSERIAL PRIMARY KEY,
    chat_id VARCHAR(50) NOT NULL UNIQUE,
    pushname VARCHAR(100),
    vinculado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_atividade TIMESTAMP,
    usuario_id BIGINT REFERENCES usuarios(id)
);

CREATE INDEX IF NOT EXISTS idx_telegram_chat_id ON telegram_vinculos(chat_id);

-- Adiciona campos para o fluxo de vinculação por código temporário
ALTER TABLE telegram_vinculos
    ADD COLUMN IF NOT EXISTS codigo_vinculo VARCHAR(20),
    ADD COLUMN IF NOT EXISTS codigo_expira  TIMESTAMP;
