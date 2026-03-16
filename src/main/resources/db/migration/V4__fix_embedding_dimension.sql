-- Recriamos a tabela preparada para o novo padrão gemini-embedding-001 (3072 dimensões)
CREATE TABLE bot_memorias_vetoriais (
    id UUID PRIMARY KEY,
    embedding VECTOR(3072),
    text TEXT,
    metadata JSONB
);

