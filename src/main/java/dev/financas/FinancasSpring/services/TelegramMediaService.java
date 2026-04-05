package dev.financas.FinancasSpring.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Serviço responsável por:
 * 1. Baixar arquivos de mídia do Telegram (áudio e imagens)
 * 2. Transcrever áudios via Gemini multimodal (REST API)
 * 3. Extrair dados de comprovantes via Gemini Vision (REST API)
 */
@Slf4j
@Service
public class TelegramMediaService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${bot.telegram.token}")
    private String telegramToken;

    @Value("${bot.gemini.api-key}")
    private String geminiApiKey;

    @Value("${bot.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    private static final String TELEGRAM_FILE_URL = "https://api.telegram.org/bot%s/getFile?file_id=%s";
    private static final String TELEGRAM_DOWNLOAD_URL = "https://api.telegram.org/file/bot%s/%s";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    public TelegramMediaService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Baixa um arquivo do Telegram pelo fileId e retorna os bytes.
     */
    public byte[] baixarArquivo(String fileId) {
        try {
            // 1. Obter o file_path do Telegram
            String getFileUrl = String.format(TELEGRAM_FILE_URL, telegramToken, fileId);
            String fileInfo = webClient.get()
                    .uri(getFileUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            JsonNode root = objectMapper.readTree(fileInfo);
            String filePath = root.path("result").path("file_path").asText();

            if (filePath.isBlank()) {
                throw new RuntimeException("file_path não retornado pelo Telegram para fileId=" + fileId);
            }

            // 2. Baixar o arquivo
            String downloadUrl = String.format(TELEGRAM_DOWNLOAD_URL, telegramToken, filePath);
            return webClient.get()
                    .uri(downloadUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

        } catch (Exception e) {
            log.error("[Media] Erro ao baixar arquivo do Telegram fileId={}: {}", fileId, e.getMessage());
            throw new RuntimeException("Falha ao baixar arquivo do Telegram: " + e.getMessage(), e);
        }
    }

    /**
     * Transcreve um áudio enviando os bytes como base64 para a API multimodal do Gemini.
     *
     * @param audioBytes bytes do arquivo de áudio
     * @param mimeType   tipo MIME do áudio (ex: audio/ogg; codecs=opus, audio/mp4)
     * @return texto transcrito
     */
    public String transcreverAudio(byte[] audioBytes, String mimeType) {
        try {
            String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);
            String url = String.format(GEMINI_API_URL, geminiModel, geminiApiKey);

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", "Transcreva exatamente o que foi dito neste áudio em português do Brasil. " +
                                       "Retorne APENAS a transcrição, sem comentários adicionais."),
                        Map.of("inline_data", Map.of(
                            "mime_type", normalizarMimeTypeAudio(mimeType),
                            "data", audioBase64
                        ))
                    ))
                )
            );

            String resposta = webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            return extrairTextoResposta(resposta);

        } catch (Exception e) {
            log.error("[Media] Erro ao transcrever áudio: {}", e.getMessage(), e);
            throw new RuntimeException("Não consegui transcrever o áudio: " + e.getMessage(), e);
        }
    }

    /**
     * Lê um comprovante/recibo em uma imagem e extrai os dados financeiros usando Gemini Vision.
     *
     * @param imagemBytes bytes da imagem
     * @param mimeType    tipo MIME da imagem (ex: image/jpeg, image/png)
     * @return texto com os dados extraídos do comprovante no formato estruturado
     */
    public String lerComprovante(byte[] imagemBytes, String mimeType) {
        try {
            String imagemBase64 = Base64.getEncoder().encodeToString(imagemBytes);
            String url = String.format(GEMINI_API_URL, geminiModel, geminiApiKey);

            String prompt = """
                Você é um leitor especialista de comprovantes financeiros.
                Analise esta imagem e extraia os dados financeiros presentes.
                
                Retorne APENAS um bloco de texto com exatamente este formato (sem markdown, sem JSON):
                ESTABELECIMENTO: [nome do estabelecimento ou loja]
                VALOR: [valor numérico em reais, ex: 49.90]
                DATA: [data no formato dd/MM/yyyy, ou "hoje" se não visível]
                CATEGORIA: [uma das opções: ALIMENTACAO, TRANSPORTE, SAUDE, MORADIA, LAZER, EDUCACAO, OUTROS]
                
                Se algum dado não for visível ou identificável, use "DESCONHECIDO" para esse campo.
                """;

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt),
                        Map.of("inline_data", Map.of(
                            "mime_type", mimeType,
                            "data", imagemBase64
                        ))
                    ))
                )
            );

            String resposta = webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            return extrairTextoResposta(resposta);

        } catch (Exception e) {
            log.error("[Media] Erro ao ler comprovante: {}", e.getMessage(), e);
            throw new RuntimeException("Não consegui ler a imagem: " + e.getMessage(), e);
        }
    }

    /**
     * Extrai o texto da resposta JSON do Gemini.
     */
    private String extrairTextoResposta(String jsonResposta) {
        try {
            JsonNode root = objectMapper.readTree(jsonResposta);
            return root
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText()
                .trim();
        } catch (Exception e) {
            log.error("[Media] Erro ao parsear resposta do Gemini: {}\nResposta: {}", e.getMessage(), jsonResposta);
            throw new RuntimeException("Resposta inesperada do Gemini.", e);
        }
    }

    /**
     * Normaliza o MIME type do áudio para um formato suportado pelo Gemini.
     * O Telegram envia áudio como audio/ogg; codecs=opus — o Gemini precisa de audio/ogg.
     */
    private String normalizarMimeTypeAudio(String mimeType) {
        if (mimeType == null) return "audio/ogg";
        // Remove parâmetros adicionais como "; codecs=opus"
        int semicolon = mimeType.indexOf(';');
        if (semicolon > 0) {
            return mimeType.substring(0, semicolon).trim();
        }
        return mimeType;
    }
}
