-- Migration V6: Criar tabelas faltantes para as entidades
-- Esta migration consolida as tabelas que estavam sendo criadas automaticamente pelo Hibernate em ambiente local

-- 1. Tabela bot_memorias (Memória de conversas do bot)
CREATE TABLE bot_memorias (
    id BIGSERIAL PRIMARY KEY,
    chat_id VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    conteudo TEXT NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX idx_memoria_chat_id ON bot_memorias(chat_id);
CREATE INDEX idx_memoria_criado_em ON bot_memorias(criado_em);

-- 2. Tabela usuario_financeiro (Detalhes financeiros do usuário)
CREATE TABLE usuario_financeiro (
    id BIGSERIAL PRIMARY KEY,
    profissao VARCHAR(100),
    renda_mensal NUMERIC(10, 2),
    tipo_renda VARCHAR(50),
    objetivo_financeiro VARCHAR(255),
    meta_poupanca_mensal NUMERIC(10, 2),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id)
);

-- 3. Tabela usuario_preferencias (Configurações do usuário)
CREATE TABLE usuario_preferencias (
    id BIGSERIAL PRIMARY KEY,
    tema_interface VARCHAR(20) NOT NULL DEFAULT 'CLARO',
    notificacoes_ativadas BOOLEAN NOT NULL DEFAULT TRUE,
    moeda_preferida VARCHAR(10) DEFAULT 'BRL',
    avatar_url VARCHAR(500),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id)
);

-- 4. Tabela usuario_detalhes (Dados pessoais)
CREATE TABLE usuario_detalhes (
    id BIGSERIAL PRIMARY KEY,
    data_nascimento DATE,
    genero VARCHAR(50),
    telefone VARCHAR(50),
    cpf VARCHAR(14) UNIQUE,
    cep VARCHAR(9),
    endereco VARCHAR(255),
    numero VARCHAR(10),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(2),
    usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id)
);
CREATE INDEX idx_usuario_detalhes_usuario_id ON usuario_detalhes(usuario_id);
CREATE INDEX idx_usuario_detalhes_cpf ON usuario_detalhes(cpf);

-- 5. Tabela demandas (Prioridades e quereres)
CREATE TABLE demandas (
    id BIGSERIAL PRIMARY KEY,
    prioridades VARCHAR(50),
    quereres VARCHAR(50),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id)
);
CREATE INDEX idx_demandas_usuario_id ON demandas(usuario_id);

-- 6. Tabela gastos (Lançamentos de despesas)
CREATE TABLE gastos (
    id BIGSERIAL PRIMARY KEY,
    estabelecimento VARCHAR(255) NOT NULL,
    descricao TEXT,
    valor NUMERIC(10, 2) NOT NULL,
    categoria VARCHAR(20) NOT NULL,
    data_gasto DATE NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    telegram_vinculo_id BIGINT NOT NULL REFERENCES telegram_vinculos(id)
);
CREATE INDEX idx_gastos_telegram_vinculo ON gastos(telegram_vinculo_id);
CREATE INDEX idx_gastos_data ON gastos(data_gasto);
CREATE INDEX idx_gastos_categoria ON gastos(categoria);

-- 7. Tabela economias (Lançamentos de poupança/investimento)
CREATE TABLE economias (
    id BIGSERIAL PRIMARY KEY,
    valor NUMERIC(10, 2) NOT NULL,
    descricao VARCHAR(255),
    data_economia DATE NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    telegram_vinculo_id BIGINT NOT NULL REFERENCES telegram_vinculos(id)
);
CREATE INDEX idx_economias_telegram_vinculo ON economias(telegram_vinculo_id);
CREATE INDEX idx_economias_data ON economias(data_economia);

-- 8. Tabela orcamentos_categoria (Metas por categoria)
CREATE TABLE orcamentos_categoria (
    id BIGSERIAL PRIMARY KEY,
    categoria VARCHAR(20) NOT NULL,
    valor_limite NUMERIC(10, 2) NOT NULL,
    telegram_vinculo_id BIGINT NOT NULL REFERENCES telegram_vinculos(id),
    UNIQUE (telegram_vinculo_id, categoria)
);
CREATE INDEX idx_orc_vinculo_categoria ON orcamentos_categoria(telegram_vinculo_id, categoria);
