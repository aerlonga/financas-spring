package dev.financas.FinancasSpring.bot;

import dev.financas.FinancasSpring.model.dto.TelegramMessageDTO;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.services.AiAssistantService;
import dev.financas.FinancasSpring.services.MessageQueueService;
import dev.financas.FinancasSpring.services.TelegramVinculoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

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

    @InjectMocks
    private TelegramMessageConsumer telegramMessageConsumer;

    @Test
    void processarMensagemUsuárioAutenticado_DeveChamarAI() {
        // Given
        TelegramMessageDTO mensagem = new TelegramMessageDTO("123", "João", "Olá", Instant.now());
        TelegramVinculo vinculo = mock(TelegramVinculo.class);
        when(vinculo.getUsuario()).thenReturn(new dev.financas.FinancasSpring.model.entities.Usuario()); // Mock Simples de Usuário

        when(vinculoService.obterOuCriar(anyString(), anyString())).thenReturn(vinculo);
        when(aiService.processar(anyString(), anyString(), anyString())).thenReturn("Resposta IA");
        doReturn(mock(java.util.concurrent.ScheduledFuture.class)).when(messageQueueService).iniciarTyping(anyString(), any());

        // When
        telegramMessageConsumer.processarMensagem(mensagem);

        // Then
        verify(aiService).processar(eq("123"), eq("João"), eq("Olá"));
        verify(financeiroBot).enviarResposta(eq("123"), eq("Resposta IA"));
    }

    @Test
    void processarMensagemUsuárioNãoAutenticado_DeveIniciarFluxoAuth() {
        // Given
        TelegramMessageDTO mensagem = new TelegramMessageDTO("123", "João", "Olá", Instant.now());
        TelegramVinculo vinculo = mock(TelegramVinculo.class);
        when(vinculo.getUsuario()).thenReturn(null); // Sem usuário vinculado

        when(vinculoService.obterOuCriar(anyString(), anyString())).thenReturn(vinculo);

        // When
        telegramMessageConsumer.processarMensagem(mensagem);

        // Then
        verify(financeiroBot).processarFluxoAutenticacao(eq("123"), eq("João"), eq("Olá"));
        verifyNoInteractions(aiService);
    }
}
