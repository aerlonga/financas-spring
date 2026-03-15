package dev.financas.FinancasSpring.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import dev.financas.FinancasSpring.bot.FinanceiroBot;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {

    private final FinanceiroBot financeiroBot;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(financeiroBot);
        return api;
    }
}
