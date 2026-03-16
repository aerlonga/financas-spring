package dev.financas.FinancasSpring.configuration;

import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class AiConfig {

    // ---- Groq (chat) ----
    @Value("${bot.groq.api-key}")
    private String groqApiKey;

    @Value("${bot.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    // @Value("${bot.groq.model:llama-3.1-8b-instant}")
    // private String groqModel;

    @Value("${bot.groq.max-tokens:1024}")
    private int groqMaxTokens;

    @Value("${bot.groq.temperature:0.3}")
    private double groqTemperature;

    // ---- Gemini (embedding) ----
    @Value("${bot.gemini.api-key}")
    private String geminiApiKey;

    @Bean
    public OpenAiChatModel groqChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .apiKey(groqApiKey)
                .modelName(groqModel)
                .maxTokens(groqMaxTokens)
                .temperature(groqTemperature)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public GoogleAiEmbeddingModel geminiEmbeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-embedding-001")
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            @Value("${DB_HOST:localhost}") String host,
            @Value("${DB_PORT:5432}") int port,
            @Value("${DB_NAME:financas}") String database,
            @Value("${DB_USERNAME:financas_user}") String user,
            @Value("${DB_PASSWORD:financas_senha}") String password) {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .table("bot_memorias_vetoriais")
                .dimension(768)
                .build();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}