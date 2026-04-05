-- ============================================================================
-- V1: Tabelas principais (H2-compatível)
-- ============================================================================

-- Tabela de usuários
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome_completo VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ATIVO',
    role VARCHAR(10) NOT NULL DEFAULT 'USER',
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(255),
    atualizado_por VARCHAR(255)
);

-- Tabela para vínculos do Telegram
CREATE TABLE IF NOT EXISTS telegram_vinculos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_id VARCHAR(50) NOT NULL UNIQUE,
    pushname VARCHAR(100),
    vinculado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_atividade TIMESTAMP,
    codigo_vinculo VARCHAR(20),
    codigo_expira TIMESTAMP,
    usuario_id BIGINT,
    CONSTRAINT fk_vinculo_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX IF NOT EXISTS idx_telegram_chat_id ON telegram_vinculos(chat_id);

-- Tabela de detalhes do usuário
CREATE TABLE IF NOT EXISTS usuario_detalhes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_nascimento DATE,
    genero VARCHAR(255),
    telefone VARCHAR(255),
    cpf VARCHAR(14) UNIQUE,
    cep VARCHAR(9),
    endereco VARCHAR(255),
    numero VARCHAR(10),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(2),
    usuario_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_detalhes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Tabela financeiro do usuário
CREATE TABLE IF NOT EXISTS usuario_financeiro (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    profissao VARCHAR(100),
    renda_mensal DECIMAL(10,2),
    tipo_renda VARCHAR(50),
    objetivo_financeiro VARCHAR(255),
    meta_poupanca_mensal DECIMAL(10,2),
    usuario_id BIGINT NOT NULL,
    CONSTRAINT fk_financeiro_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Tabela preferências do usuário
CREATE TABLE IF NOT EXISTS usuario_preferencias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tema_interface VARCHAR(20) NOT NULL DEFAULT 'CLARO',
    notificacoes_ativadas BOOLEAN NOT NULL DEFAULT TRUE,
    moeda_preferida VARCHAR(10) DEFAULT 'BRL',
    avatar_url VARCHAR(500),
    usuario_id BIGINT NOT NULL,
    CONSTRAINT fk_preferencias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Tabela de demandas
CREATE TABLE IF NOT EXISTS demandas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prioridades VARCHAR(50),
    quereres VARCHAR(50),
    usuario_id BIGINT NOT NULL,
    CONSTRAINT fk_demandas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Tabela de gastos
CREATE TABLE IF NOT EXISTS gastos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    estabelecimento VARCHAR(255) NOT NULL,
    descricao CLOB,
    valor DECIMAL(10,2) NOT NULL,
    categoria VARCHAR(20) NOT NULL,
    data_gasto DATE NOT NULL,
    criado_em TIMESTAMP NOT NULL,
    telegram_vinculo_id BIGINT NOT NULL,
    CONSTRAINT fk_gasto_vinculo FOREIGN KEY (telegram_vinculo_id) REFERENCES telegram_vinculos(id)
);

CREATE INDEX IF NOT EXISTS idx_gastos_telegram_vinculo ON gastos(telegram_vinculo_id);
CREATE INDEX IF NOT EXISTS idx_gastos_data ON gastos(data_gasto);
CREATE INDEX IF NOT EXISTS idx_gastos_categoria ON gastos(categoria);

-- Tabela de economias
CREATE TABLE IF NOT EXISTS economias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    valor DECIMAL(10,2) NOT NULL,
    descricao VARCHAR(255),
    data_economia DATE NOT NULL,
    criado_em TIMESTAMP NOT NULL,
    telegram_vinculo_id BIGINT NOT NULL,
    CONSTRAINT fk_economia_vinculo FOREIGN KEY (telegram_vinculo_id) REFERENCES telegram_vinculos(id)
);

CREATE INDEX IF NOT EXISTS idx_economias_telegram_vinculo ON economias(telegram_vinculo_id);
CREATE INDEX IF NOT EXISTS idx_economias_data ON economias(data_economia);

-- Tabela de orçamentos por categoria
CREATE TABLE IF NOT EXISTS orcamentos_categoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria VARCHAR(20) NOT NULL,
    valor_limite DECIMAL(10,2) NOT NULL,
    telegram_vinculo_id BIGINT NOT NULL,
    CONSTRAINT fk_orcamento_vinculo FOREIGN KEY (telegram_vinculo_id) REFERENCES telegram_vinculos(id),
    CONSTRAINT uq_orc_vinculo_categoria UNIQUE (telegram_vinculo_id, categoria)
);

-- Tabela de memórias do bot
CREATE TABLE IF NOT EXISTS bot_memorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_id VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    conteudo CLOB NOT NULL,
    criado_em TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_memoria_chat_id ON bot_memorias(chat_id);
CREATE INDEX IF NOT EXISTS idx_memoria_criado_em ON bot_memorias(criado_em);

-- Tabela de vetores (simplificada – H2 não suporta VECTOR, usamos CLOB como placeholder)
CREATE TABLE IF NOT EXISTS bot_memorias_vetoriais (
    embedding_id UUID PRIMARY KEY,
    embedding CLOB,
    text CLOB,
    metadata CLOB
);
