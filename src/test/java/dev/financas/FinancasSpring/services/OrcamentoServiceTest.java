package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.OrcamentoCategoria;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.repository.GastoRepository;
import dev.financas.FinancasSpring.repository.OrcamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private GastoRepository gastoRepository;

    @InjectMocks
    private OrcamentoService orcamentoService;

    private TelegramVinculo criarVinculo() {
        return TelegramVinculo.builder().id(1L).chatId("123").build();
    }

    @Test
    void deveConsultarOrcamentoPorCategoriaComLimite() {
        TelegramVinculo vinculo = criarVinculo();
        OrcamentoCategoria orc = OrcamentoCategoria.builder()
                .id(1L).categoria(CategoriaGasto.ALIMENTACAO)
                .valorLimite(BigDecimal.valueOf(500)).telegramVinculo(vinculo).build();

        when(orcamentoRepository.findByTelegramVinculoAndCategoria(vinculo, CategoriaGasto.ALIMENTACAO))
                .thenReturn(Optional.of(orc));
        when(gastoRepository.somarPorPeriodoECategoria(eq(vinculo), any(), any(), eq(CategoriaGasto.ALIMENTACAO)))
                .thenReturn(BigDecimal.valueOf(200));

        String resultado = orcamentoService.consultarOrcamento(vinculo, CategoriaGasto.ALIMENTACAO);

        assertThat(resultado).contains("Limite: R$ 500,00");
        assertThat(resultado).contains("Gasto: R$ 200,00");
        assertThat(resultado).contains("Saldo: R$ 300,00");
    }

    @Test
    void deveIndicarOrcamentoEstourado() {
        TelegramVinculo vinculo = criarVinculo();
        OrcamentoCategoria orc = OrcamentoCategoria.builder()
                .id(1L).categoria(CategoriaGasto.LAZER)
                .valorLimite(BigDecimal.valueOf(100)).telegramVinculo(vinculo).build();

        when(orcamentoRepository.findByTelegramVinculoAndCategoria(vinculo, CategoriaGasto.LAZER))
                .thenReturn(Optional.of(orc));
        when(gastoRepository.somarPorPeriodoECategoria(eq(vinculo), any(), any(), eq(CategoriaGasto.LAZER)))
                .thenReturn(BigDecimal.valueOf(150));

        String resultado = orcamentoService.consultarOrcamento(vinculo, CategoriaGasto.LAZER);

        assertThat(resultado).contains("Orçamento estourado!");
    }

    @Test
    void deveInformarSemOrcamentoDefinido() {
        TelegramVinculo vinculo = criarVinculo();
        when(orcamentoRepository.findByTelegramVinculoAndCategoria(vinculo, CategoriaGasto.SAUDE))
                .thenReturn(Optional.empty());
        when(gastoRepository.somarPorPeriodoECategoria(eq(vinculo), any(), any(), eq(CategoriaGasto.SAUDE)))
                .thenReturn(BigDecimal.valueOf(50));

        String resultado = orcamentoService.consultarOrcamento(vinculo, CategoriaGasto.SAUDE);

        assertThat(resultado).contains("não definiu um orçamento");
        assertThat(resultado).contains("R$ 50,00");
    }

    @Test
    void deveConsultarVisaoGeral() {
        TelegramVinculo vinculo = criarVinculo();
        OrcamentoCategoria orc1 = OrcamentoCategoria.builder()
                .valorLimite(BigDecimal.valueOf(500)).build();
        OrcamentoCategoria orc2 = OrcamentoCategoria.builder()
                .valorLimite(BigDecimal.valueOf(300)).build();

        when(orcamentoRepository.findByTelegramVinculo(vinculo)).thenReturn(List.of(orc1, orc2));
        when(gastoRepository.somarPorPeriodo(eq(vinculo), any(), any())).thenReturn(BigDecimal.valueOf(400));

        String resultado = orcamentoService.consultarOrcamento(vinculo, null);

        assertThat(resultado).contains("Orçamento total: R$ 800,00");
        assertThat(resultado).contains("Total gasto: R$ 400,00");
    }

    @Test
    void deveConsultarVisaoGeralSemOrcamentos() {
        TelegramVinculo vinculo = criarVinculo();
        when(orcamentoRepository.findByTelegramVinculo(vinculo)).thenReturn(List.of());
        when(gastoRepository.somarPorPeriodo(eq(vinculo), any(), any())).thenReturn(BigDecimal.valueOf(100));

        String resultado = orcamentoService.consultarOrcamento(vinculo, null);

        assertThat(resultado).contains("Nenhum orçamento configurado");
    }

    @Test
    void deveDefinirNovoOrcamento() {
        TelegramVinculo vinculo = criarVinculo();
        when(orcamentoRepository.findByTelegramVinculoAndCategoria(vinculo, CategoriaGasto.ALIMENTACAO))
                .thenReturn(Optional.empty());
        when(orcamentoRepository.save(any(OrcamentoCategoria.class))).thenAnswer(inv -> inv.getArgument(0));

        String resultado = orcamentoService.definirOrcamento(vinculo, CategoriaGasto.ALIMENTACAO, BigDecimal.valueOf(500));

        assertThat(resultado).contains("R$ 500,00");
        assertThat(resultado).contains("ALIMENTACAO");
        verify(orcamentoRepository).save(any());
    }

    @Test
    void deveAtualizarOrcamentoExistente() {
        TelegramVinculo vinculo = criarVinculo();
        OrcamentoCategoria existente = OrcamentoCategoria.builder()
                .id(1L).categoria(CategoriaGasto.ALIMENTACAO)
                .valorLimite(BigDecimal.valueOf(300)).telegramVinculo(vinculo).build();

        when(orcamentoRepository.findByTelegramVinculoAndCategoria(vinculo, CategoriaGasto.ALIMENTACAO))
                .thenReturn(Optional.of(existente));
        when(orcamentoRepository.save(any(OrcamentoCategoria.class))).thenReturn(existente);

        String resultado = orcamentoService.definirOrcamento(vinculo, CategoriaGasto.ALIMENTACAO, BigDecimal.valueOf(600));

        assertThat(existente.getValorLimite()).isEqualByComparingTo(BigDecimal.valueOf(600));
        verify(orcamentoRepository).save(existente);
    }
}
