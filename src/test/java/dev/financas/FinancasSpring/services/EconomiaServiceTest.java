package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.model.entities.Economia;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.repository.EconomiaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EconomiaServiceTest {

    @Mock
    private EconomiaRepository repository;

    @InjectMocks
    private EconomiaService economiaService;

    private TelegramVinculo criarVinculo() {
        return TelegramVinculo.builder().id(1L).chatId("123").build();
    }

    @Test
    void deveRegistrarEconomiaComSucesso() {
        TelegramVinculo vinculo = criarVinculo();
        when(repository.save(any(Economia.class))).thenAnswer(inv -> inv.getArgument(0));

        Economia resultado = economiaService.registrar(vinculo, BigDecimal.valueOf(500), "Salário");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getValor()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(resultado.getDescricao()).isEqualTo("Salário");
        verify(repository).save(any(Economia.class));
    }

    @Test
    void deveConsultarTotalComPeriodo() {
        TelegramVinculo vinculo = criarVinculo();
        LocalDate inicio = LocalDate.of(2026, 4, 1);
        LocalDate fim = LocalDate.of(2026, 4, 30);

        when(repository.somarPorPeriodo(vinculo, inicio, fim)).thenReturn(BigDecimal.valueOf(1500));

        String resultado = economiaService.consultarTotal(vinculo, inicio, fim);

        assertThat(resultado).contains("R$ 1500,00");
        assertThat(resultado).contains("2026-04-01");
    }

    @Test
    void deveConsultarTotalGeral() {
        TelegramVinculo vinculo = criarVinculo();
        when(repository.somarTotal(vinculo)).thenReturn(BigDecimal.valueOf(5000));

        String resultado = economiaService.consultarTotal(vinculo, null, null);

        assertThat(resultado).contains("R$ 5000,00");
        assertThat(resultado).contains("todo o histórico");
    }
}
