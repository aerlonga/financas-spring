package dev.financas.FinancasSpring.bot.tools;

import dev.financas.FinancasSpring.model.entities.Gasto;
import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.services.EconomiaService;
import dev.financas.FinancasSpring.services.GastoService;
import dev.financas.FinancasSpring.services.OrcamentoService;
import dev.financas.FinancasSpring.services.TelegramVinculoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceiroToolsTest {

    @Mock
    private GastoService gastoService;

    @Mock
    private EconomiaService economiaService;

    @Mock
    private OrcamentoService orcamentoService;

    @Mock
    private TelegramVinculoService vinculoService;

    @InjectMocks
    private FinanceiroTools financeiroTools;

    private final TelegramVinculo vinculo = TelegramVinculo.builder().id(1L).chatId("123").build();

    @BeforeEach
    void setUp() {
        FinanceiroTools.setChatId("123");
        // lenient: deveLancarExcecaoSemChatId clears chatId before calling, so this stub is never used in that test
        lenient().when(vinculoService.obterOuCriar("123", "sistema")).thenReturn(vinculo);
    }

    @AfterEach
    void tearDown() {
        FinanceiroTools.clearChatId();
    }

    // ─── registrarGasto ────────────────────────────────────────────────

    @Test
    void deveRegistrarGastoComSucesso() {
        Gasto gasto = Gasto.builder().id(1L).build();
        when(gastoService.registrar(any(), anyString(), any(), any(), any(), any())).thenReturn(gasto);

        String resultado = financeiroTools.registrarGasto("Padaria", 25.50, "ALIMENTACAO", "05/04/2026", "Pão");

        assertThat(resultado).contains("Gasto registrado");
        assertThat(resultado).contains("Padaria");
        assertThat(resultado).contains("R$ 25,50");
    }

    @Test
    void deveRegistrarGastoComCategoriaInvalida() {
        String resultado = financeiroTools.registrarGasto("Padaria", 25.50, "INVALIDA", null, null);

        assertThat(resultado).contains("Categoria inválida");
    }

    @Test
    void deveRegistrarGastoSemData() {
        Gasto gasto = Gasto.builder().id(1L).build();
        when(gastoService.registrar(any(), anyString(), any(), any(), any(), any())).thenReturn(gasto);

        String resultado = financeiroTools.registrarGasto("Uber", 30.0, "TRANSPORTE", null, null);

        assertThat(resultado).contains("Gasto registrado");
        verify(gastoService).registrar(eq(vinculo), eq("Uber"), any(), eq(CategoriaGasto.TRANSPORTE), eq(LocalDate.now()), isNull());
    }

    @Test
    void deveRegistrarGastoComDescricao() {
        Gasto gasto = Gasto.builder().id(1L).build();
        when(gastoService.registrar(any(), anyString(), any(), any(), any(), any())).thenReturn(gasto);

        String resultado = financeiroTools.registrarGasto("Mercado", 200.0, "ALIMENTACAO", "", "Arroz, Feijão, Carne");

        assertThat(resultado).contains("Descrição: Arroz, Feijão, Carne");
    }

    // ─── consultarGastos ───────────────────────────────────────────────

    @Test
    void deveConsultarGastosHoje() {
        when(gastoService.consultarResumo(any(), any(), any(), any())).thenReturn("Total gasto: R$ 50,00");

        String resultado = financeiroTools.consultarGastos("HOJE", null);

        assertThat(resultado).contains("Total gasto");
        verify(gastoService).consultarResumo(eq(vinculo), eq(LocalDate.now()), eq(LocalDate.now()), isNull());
    }

    @Test
    void deveConsultarGastosPorMes() {
        when(gastoService.consultarResumo(any(), any(), any(), any())).thenReturn("Total gasto: R$ 500,00");

        financeiroTools.consultarGastos("MES", "");

        verify(gastoService).consultarResumo(eq(vinculo), eq(LocalDate.now().withDayOfMonth(1)), eq(LocalDate.now()), isNull());
    }

    @Test
    void deveConsultarGastosPorDataEspecifica() {
        when(gastoService.consultarResumo(any(), any(), any(), any())).thenReturn("OK");

        financeiroTools.consultarGastos("01/04/2026", null);

        LocalDate esperada = LocalDate.of(2026, 4, 1);
        verify(gastoService).consultarResumo(eq(vinculo), eq(esperada), eq(esperada), isNull());
    }

    @Test
    void deveConsultarGastosPorIntervalo() {
        when(gastoService.consultarResumo(any(), any(), any(), any())).thenReturn("OK");

        financeiroTools.consultarGastos("01/04/2026-05/04/2026", null);

        verify(gastoService).consultarResumo(eq(vinculo),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 5)),
                isNull());
    }

    @Test
    void deveConsultarGastosComCategoria() {
        when(gastoService.consultarResumo(any(), any(), any(), any())).thenReturn("OK");

        financeiroTools.consultarGastos("MES", "ALIMENTACAO");

        verify(gastoService).consultarResumo(any(), any(), any(), eq(CategoriaGasto.ALIMENTACAO));
    }

    // ─── registrarEconomia ─────────────────────────────────────────────

    @Test
    void deveRegistrarEconomia() {
        String resultado = financeiroTools.registrarEconomia(500.0, "Salário");

        assertThat(resultado).contains("Economia de R$ 500,00 registrada");
        verify(economiaService).registrar(eq(vinculo), eq(BigDecimal.valueOf(500.0)), eq("Salário"));
    }

    // ─── consultarEconomias ────────────────────────────────────────────

    @Test
    void deveConsultarEconomiasTotal() {
        when(economiaService.consultarTotal(vinculo, null, null)).thenReturn("Total: R$ 1000,00");

        String resultado = financeiroTools.consultarEconomias("TOTAL");

        assertThat(resultado).contains("R$ 1000,00");
    }

    @Test
    void deveConsultarEconomiasPorMes() {
        when(economiaService.consultarTotal(any(), any(), any())).thenReturn("OK");

        financeiroTools.consultarEconomias("MES");

        verify(economiaService).consultarTotal(eq(vinculo), eq(LocalDate.now().withDayOfMonth(1)), eq(LocalDate.now()));
    }

    // ─── consultarOrcamento ────────────────────────────────────────────

    @Test
    void deveConsultarOrcamentoGeral() {
        when(orcamentoService.consultarOrcamento(vinculo, null)).thenReturn("Resumo do mês");

        String resultado = financeiroTools.consultarOrcamento(null);

        assertThat(resultado).contains("Resumo");
    }

    @Test
    void deveConsultarOrcamentoPorCategoria() {
        when(orcamentoService.consultarOrcamento(vinculo, CategoriaGasto.ALIMENTACAO))
                .thenReturn("Orçamento ALIMENTACAO");

        String resultado = financeiroTools.consultarOrcamento("ALIMENTACAO");

        assertThat(resultado).contains("ALIMENTACAO");
    }

    // ─── definirOrcamento ──────────────────────────────────────────────

    @Test
    void deveDefinirOrcamento() {
        when(orcamentoService.definirOrcamento(any(), any(), any())).thenReturn("Orçamento de R$ 500,00 definido");

        String resultado = financeiroTools.definirOrcamento("ALIMENTACAO", 500.0);

        assertThat(resultado).contains("R$ 500,00");
    }

    // ─── apagarUltimoGasto ─────────────────────────────────────────────

    @Test
    void deveApagarUltimoGasto() {
        when(gastoService.apagarUltimoGasto(vinculo)).thenReturn("Gasto apagado com sucesso!");

        String resultado = financeiroTools.apagarUltimoGasto();

        assertThat(resultado).contains("apagado com sucesso");
    }

    // ─── apagarGastoPorId ──────────────────────────────────────────────

    @Test
    void deveApagarGastoPorId() {
        when(gastoService.apagarGastoPorId(vinculo, 42L)).thenReturn("Gasto apagado!");

        String resultado = financeiroTools.apagarGastoPorId(42L);

        assertThat(resultado).contains("apagado");
    }

    // ─── editarGasto ───────────────────────────────────────────────────

    @Test
    void deveEditarGasto() {
        when(gastoService.editarGasto(any(), anyLong(), any(), any(), any(), any(), any()))
                .thenReturn("Gasto atualizado com sucesso!");

        String resultado = financeiroTools.editarGasto(1L, "Novo Local", 50.0, "TRANSPORTE", "01/04/2026", "Nova desc");

        assertThat(resultado).contains("atualizado");
    }

    @Test
    void deveEditarGastoComCamposVazios() {
        when(gastoService.editarGasto(any(), anyLong(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn("Gasto atualizado com sucesso!");

        String resultado = financeiroTools.editarGasto(1L, "", 0.0, "", "", "");

        assertThat(resultado).contains("atualizado");
        verify(gastoService).editarGasto(eq(vinculo), eq(1L), isNull(), isNull(), isNull(), isNull(), eq(""));
    }

    // ─── ThreadLocal chatId ────────────────────────────────────────────

    @Test
    void deveLancarExcecaoSemChatId() {
        FinanceiroTools.clearChatId();
        // Sem chatId no contexto, deve lançar IllegalStateException via getVinculo()
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> financeiroTools.apagarUltimoGasto());
    }
}
