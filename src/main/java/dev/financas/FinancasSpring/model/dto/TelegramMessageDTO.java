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

    @JsonProperty("chatId")
    private String chatId;

    @JsonProperty("nome")
    private String nome;

    @JsonProperty("texto")
    private String texto;

    @JsonProperty("timestamp")
    private Instant timestamp;

    public TelegramMessageDTO(String chatId, String nome, String texto) {
        this.chatId = chatId;
        this.nome = nome;
        this.texto = texto;
        this.timestamp = Instant.now();
    }
}
