package dev.financas.FinancasSpring.configuration;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GeminiConfig {

    @Value("${bot.gemini.api-key}")
    private String apiKey;

    @Value("${bot.gemini.model:gemini-3.1-flash}")
    private String model;

    @Value("${bot.gemini.max-tokens:1024}")
    private int maxTokens;

    @Value("${bot.gemini.temperature:0.3}")
    private double temperature;

    @Bean
    public GoogleAiGeminiChatModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName(model)
            .maxOutputTokens(maxTokens)
            .temperature(temperature)
            .build();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
