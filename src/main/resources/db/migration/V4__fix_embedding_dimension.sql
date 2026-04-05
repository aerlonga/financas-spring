DROP TABLE IF EXISTS bot_memorias_vetoriais CASCADE;

CREATE TABLE bot_memorias_vetoriais (
    id UUID PRIMARY KEY,
    embedding VECTOR(3072),
    text TEXT,
    metadata JSONB
);

-- CREATE INDEX ON bot_memorias_vetoriais USING hnsw (embedding vector_cosine_ops);



