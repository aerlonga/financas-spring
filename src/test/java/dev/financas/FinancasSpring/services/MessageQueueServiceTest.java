package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.configuration.RabbitMQConfig;
import dev.financas.FinancasSpring.model.dto.TelegramMessageDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageQueueServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MessageQueueService messageQueueService;

    @Test
    void submeterDevePublicarMensagemNoRabbitMQ() {
        // Given
        String chatId = "12345";
        String nome = "João";
        String texto = "Olá bot";

        // When
        messageQueueService.submeter(chatId, nome, texto);

        // Then
        ArgumentCaptor<TelegramMessageDTO> captor = ArgumentCaptor.forClass(TelegramMessageDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.ROUTING_KEY),
                captor.capture()
        );

        TelegramMessageDTO mensagemEnviada = captor.getValue();
        assertEquals(chatId, mensagemEnviada.getChatId());
        assertEquals(nome, mensagemEnviada.getNome());
        assertEquals(texto, mensagemEnviada.getTexto());
    }
}
