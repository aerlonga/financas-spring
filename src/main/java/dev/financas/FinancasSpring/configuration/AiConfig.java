package dev.financas.FinancasSpring.configuration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiConfig {

    // ---- Gemini (Google AI) ----
    @Value("${bot.gemini.api-key}")
    private String geminiApiKey;

    @Value("${bot.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${bot.gemini.max-tokens:1024}")
    private int geminiMaxTokens;

    @Value("${bot.gemini.temperature:0.3}")
    private double geminiTemperature;

    /**
     * Modelo de Chat principal usando Google Gemini.
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModel)
                .maxOutputTokens(geminiMaxTokens)
                .temperature(geminiTemperature)
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