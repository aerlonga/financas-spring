package dev.financas.FinancasSpring.bot.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Tool de pesquisa web: cotações via AwesomeAPI e busca genérica.
 */
@Component
@RequiredArgsConstructor
public class PesquisaWebTools {

    private final WebClient webClient;

    @Value("${bot.currency.api-url}")
    private String currencyApiUrl;

    private static final Map<String, String> PARES_MOEDA = Map.of(
        "dolar", "USD-BRL",
        "euro", "EUR-BRL",
        "libra", "GBP-BRL",
        "bitcoin", "BTC-BRL",
        "ethereum", "ETH-BRL"
    );

    @Tool("Busca cotação atual de moeda ou criptomoeda. Use para perguntas como 'quanto está o dólar', 'preço do bitcoin', etc.")
    @SuppressWarnings("unchecked")
    public String consultarCotacao(
        @P("Nome da moeda: dolar, euro, libra, bitcoin, ethereum") String moeda
    ) {
        String par = PARES_MOEDA.getOrDefault(moeda.toLowerCase(), "USD-BRL");
        try {
            String url = currencyApiUrl + "/" + par;
            Map response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(15))
                .block();

            if (response == null) return "Não consegui buscar a cotação no momento.";

            String chave = par.replace("-", "");
            Map<String, Object> dados = (Map<String, Object>) response.get(chave);
            if (dados == null) return "Cotação não disponível para " + moeda;

            return String.format(
                "Cotação %s:\n• Compra: R$ %s\n• Venda: R$ %s\n• Variação: %s%%\nFonte: AwesomeAPI",
                dados.get("name"),
                dados.get("bid"),
                dados.get("ask"),
                dados.get("pctChange")
            );
        } catch (Exception e) {
            return "Erro ao buscar cotação: " + e.getMessage();
        }
    }

    @Tool("Pesquisa informações financeiras atualizadas na internet. Use para taxas (Selic, CDI, IPCA), notícias do mercado ou qualquer dado que possa ter mudado recentemente.")
    @SuppressWarnings("unchecked")
    public String pesquisarWeb(
        @P("Termo de busca financeiro (ex: 'taxa selic atual', 'IPCA março 2026')") String query
    ) {
        try {
            // DuckDuckGo Instant Answer API (pública, sem autenticação)
            String url = "https://api.duckduckgo.com/?q=" +
                java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) +
                "&format=json&no_html=1&skip_disambig=1";

            Map response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(15))
                .block();

            if (response == null) return "Não encontrei resultados para: " + query;

            String abstractText = (String) response.getOrDefault("AbstractText", "");
            String answer = (String) response.getOrDefault("Answer", "");

            if (!answer.isBlank()) return "Resultado: " + answer;
            if (!abstractText.isBlank()) return abstractText;

            return "Não encontrei um resultado direto para '" + query + "'. Tente reformular a pesquisa.";

        } catch (Exception e) {
            return "Erro na pesquisa: " + e.getMessage();
        }
    }
}
