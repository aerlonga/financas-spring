package dev.financas.FinancasSpring.bot;

import com.rabbitmq.client.Channel;
import dev.financas.FinancasSpring.model.dto.TelegramMessageDTO;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.services.AiAssistantService;
import dev.financas.FinancasSpring.services.GastoService;
import dev.financas.FinancasSpring.services.MessageQueueService;
import dev.financas.FinancasSpring.services.TelegramMediaService;
import dev.financas.FinancasSpring.services.TelegramVinculoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramMessageConsumerTest {

    @Mock
    private AiAssistantService aiService;

    @Mock
    private TelegramVinculoService vinculoService;

    @Mock
    private MessageQueueService messageQueueService;

    @Mock
    private FinanceiroBot financeiroBot;

    @Mock
    private BotSessionManager sessionManager;

    @Mock
    private GastoService gastoService;

    @Mock
    private TelegramMediaService mediaService;

    @InjectMocks
    private TelegramMessageConsumer telegramMessageConsumer;

    @Test
    void processarMensagemUsuárioAutenticado_DeveChamarAI() throws Exception {
        // Given
        // Mudado de "Olá" para "Gastei 50" para não cair na lógica de saudação
        TelegramMessageDTO mensagem = new TelegramMessageDTO("123", "João", "Gastei 50", Instant.now());
        TelegramVinculo vinculo = mock(TelegramVinculo.class);
        when(vinculo.getUsuario()).thenReturn(new dev.financas.FinancasSpring.model.entities.Usuario());

        when(vinculoService.obterOuCriar(anyString(), anyString())).thenReturn(vinculo);
        when(aiService.processar(anyString(), anyString(), anyString())).thenReturn("Resposta IA");
        when(sessionManager.getEstado(anyString())).thenReturn(BotSessionState.NONE);
        
        // Mock do typing indicator
        when(messageQueueService.iniciarTyping(anyString(), any())).thenReturn(mock(ScheduledFuture.class));

        Channel channel = mock(Channel.class);
        long deliveryTag = 1L;

        // When
        telegramMessageConsumer.processarMensagem(mensagem, channel, deliveryTag);

        // Then
        verify(aiService).processar(eq("123"), eq("João"), eq("Gastei 50"));
        verify(financeiroBot).enviarResposta(eq("123"), eq("Resposta IA"));
        verify(channel).basicAck(eq(deliveryTag), eq(false));
    }

    @Test
    void processarMensagemUsuárioNãoAutenticado_DeveIniciarFluxoAuth() throws Exception {
        // Given
        TelegramMessageDTO mensagem = new TelegramMessageDTO("123", "João", "Oi", Instant.now());
        TelegramVinculo vinculo = mock(TelegramVinculo.class);
        when(vinculo.getUsuario()).thenReturn(null); // Sem usuário vinculado

        // Mocks necessários
        when(vinculoService.obterOuCriar(anyString(), anyString())).thenReturn(vinculo);
        when(sessionManager.getEstado(anyString())).thenReturn(BotSessionState.NONE);

        Channel channel = mock(Channel.class);
        long deliveryTag = 1L;

        // When
        telegramMessageConsumer.processarMensagem(mensagem, channel, deliveryTag);

        // Then
        verify(financeiroBot).processarFluxoAutenticacao(eq("123"), eq("João"), eq("Oi"));
        verifyNoInteractions(aiService);
        verify(channel).basicAck(eq(deliveryTag), eq(false));
    }
}
