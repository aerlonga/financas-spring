package dev.financas.FinancasSpring.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO que representa uma mensagem do Telegram para ser enfileirada no RabbitMQ.
 * Precisa ser serializável em JSON para trafegar pela fila.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramMessageDTO implements Serializable {

    public enum TipoMensagem {
        TEXTO, AUDIO, IMAGEM
    }

    @JsonProperty("chatId")
    private String chatId;

    @JsonProperty("nome")
    private String nome;

    @JsonProperty("texto")
    private String texto;

    /** Tipo da mensagem: TEXTO, AUDIO ou IMAGEM. */
    @JsonProperty("tipo")
    private TipoMensagem tipo;

    /** Conteúdo do arquivo de mídia codificado em Base64 (para AUDIO e IMAGEM). */
    @JsonProperty("mediaBase64")
    private String mediaBase64;

    /** MIME type do arquivo de mídia (ex: audio/ogg, image/jpeg). */
    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("timestamp")
    private Instant timestamp;

    /** Construtor para mensagens de texto. */
    public TelegramMessageDTO(String chatId, String nome, String texto) {
        this.chatId = chatId;
        this.nome = nome;
        this.texto = texto;
        this.tipo = TipoMensagem.TEXTO;
        this.timestamp = Instant.now();
    }

    /** Construtor para mensagens de texto com timestamp customizado. */
    public TelegramMessageDTO(String chatId, String nome, String texto, Instant timestamp) {
        this.chatId = chatId;
        this.nome = nome;
        this.texto = texto;
        this.tipo = TipoMensagem.TEXTO;
        this.timestamp = timestamp;
    }

    /** Construtor para mensagens de mídia. */
    public TelegramMessageDTO(String chatId, String nome, TipoMensagem tipo, String mediaBase64, String mimeType) {
        this.chatId = chatId;
        this.nome = nome;
        this.texto = null;
        this.tipo = tipo;
        this.mediaBase64 = mediaBase64;
        this.mimeType = mimeType;
        this.timestamp = Instant.now();
    }
}
