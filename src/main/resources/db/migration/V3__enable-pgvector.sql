-- Habilita a extensão pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Tabela para armazenamento de embeddings (vetores) das conversas
CREATE TABLE bot_memorias_vetoriais (
    id UUID PRIMARY KEY,
    embedding VECTOR(768), -- 768 é a dimensão do Google Gemini Embedding (text-embedding-004)
    text TEXT,
    metadata JSONB
);

-- Índice para busca de vizinhos mais próximos (IVFFlat ou HNSW)
-- HNSW é geralmente mais rápido e preciso para buscas vetoriais
CREATE INDEX ON bot_memorias_vetoriais USING hnsw (embedding vector_cosine_ops);
