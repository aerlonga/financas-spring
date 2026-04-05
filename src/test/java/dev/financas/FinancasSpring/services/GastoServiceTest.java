package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.model.entities.Gasto;
import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.repository.GastoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GastoServiceTest {

    @Mock
    private GastoRepository repository;

    @InjectMocks
    private GastoService gastoService;

    private TelegramVinculo criarVinculo() {
        return TelegramVinculo.builder().id(1L).chatId("123").build();
    }

    private Gasto criarGasto(Long id, String estabelecimento, BigDecimal valor, CategoriaGasto cat) {
        return Gasto.builder()
                .id(id)
                .estabelecimento(estabelecimento)
                .valor(valor)
                .categoria(cat)
                .dataGasto(LocalDate.now())
                .criadoEm(LocalDateTime.now())
                .telegramVinculo(criarVinculo())
                .build();
    }

    @Test
    void deveRegistrarGastoComSucesso() {
        TelegramVinculo vinculo = criarVinculo();
        Gasto gastoSalvo = criarGasto(1L, "Padaria", BigDecimal.valueOf(25.50), CategoriaGasto.ALIMENTACAO);
        when(repository.save(any(Gasto.class))).thenReturn(gastoSalvo);

        Gasto resultado = gastoService.registrar(vinculo, "Padaria", BigDecimal.valueOf(25.50),
                CategoriaGasto.ALIMENTACAO, LocalDate.now(), "Pão e leite");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstabelecimento()).isEqualTo("Padaria");
        verify(repository).save(any(Gasto.class));
    }

    @Test
    void deveUsarDataAtualSeDataNull() {
        TelegramVinculo vinculo = criarVinculo();
        when(repository.save(any(Gasto.class))).thenAnswer(inv -> inv.getArgument(0));

        Gasto resultado = gastoService.registrar(vinculo, "Mercado", BigDecimal.valueOf(100),
                CategoriaGasto.ALIMENTACAO, null, null);

        assertThat(resultado.getDataGasto()).isEqualTo(LocalDate.now());
    }

    @Test
    void deveConsultarResumoComGastos() {
        TelegramVinculo vinculo = criarVinculo();
        List<Gasto> gastos = List.of(
                criarGasto(1L, "Padaria", BigDecimal.valueOf(25), CategoriaGasto.ALIMENTACAO),
                criarGasto(2L, "Uber", BigDecimal.valueOf(30), CategoriaGasto.TRANSPORTE)
        );

        when(repository.findByTelegramVinculoAndDataGastoBetweenOrderByDataGastoDesc(
                any(), any(), any())).thenReturn(gastos);
        when(repository.somarPorPeriodo(any(), any(), any())).thenReturn(BigDecimal.valueOf(55));

        String resumo = gastoService.consultarResumo(vinculo, LocalDate.now(), LocalDate.now(), null);

        assertThat(resumo).contains("Total gasto: R$ 55,00");
        assertThat(resumo).contains("Registros: 2");
    }

    @Test
    void deveRetornarMensagemSemGastos() {
        TelegramVinculo vinculo = criarVinculo();
        when(repository.findByTelegramVinculoAndDataGastoBetweenOrderByDataGastoDesc(
                any(), any(), any())).thenReturn(List.of());

        String resumo = gastoService.consultarResumo(vinculo, LocalDate.now(), LocalDate.now(), null);

        assertThat(resumo).isEqualTo("Nenhum gasto encontrado no período informado.");
    }

    @Test
    void deveConsultarResumoPorCategoria() {
        TelegramVinculo vinculo = criarVinculo();
        List<Gasto> gastos = List.of(
                criarGasto(1L, "Padaria", BigDecimal.valueOf(25), CategoriaGasto.ALIMENTACAO)
        );

        when(repository.findByTelegramVinculoAndDataGastoBetweenAndCategoriaOrderByDataGastoDesc(
                any(), any(), any(), eq(CategoriaGasto.ALIMENTACAO))).thenReturn(gastos);
        when(repository.somarPorPeriodoECategoria(any(), any(), any(), eq(CategoriaGasto.ALIMENTACAO)))
                .thenReturn(BigDecimal.valueOf(25));

        String resumo = gastoService.consultarResumo(vinculo, LocalDate.now(), LocalDate.now(), CategoriaGasto.ALIMENTACAO);

        assertThat(resumo).contains("ALIMENTACAO");
    }

    @Test
    void deveApagarUltimoGastoComSucesso() {
        TelegramVinculo vinculo = criarVinculo();
        Gasto gasto = criarGasto(1L, "Padaria", BigDecimal.valueOf(25), CategoriaGasto.ALIMENTACAO);
        when(repository.findFirstByTelegramVinculoOrderByCriadoEmDesc(vinculo)).thenReturn(Optional.of(gasto));

        String resultado = gastoService.apagarUltimoGasto(vinculo);

        assertThat(resultado).contains("Gasto apagado com sucesso");
        verify(repository).delete(gasto);
    }

    @Test
    void deveRetornarMensagemSemGastoParaApagar() {
        TelegramVinculo vinculo = criarVinculo();
        when(repository.findFirstByTelegramVinculoOrderByCriadoEmDesc(vinculo)).thenReturn(Optional.empty());

        String resultado = gastoService.apagarUltimoGasto(vinculo);

        assertThat(resultado).isEqualTo("Nenhum gasto encontrado para apagar.");
    }

    @Test
    void deveApagarGastoPorIdComSucesso() {
        TelegramVinculo vinculo = criarVinculo();
        Gasto gasto = criarGasto(1L, "Mercado", BigDecimal.valueOf(200), CategoriaGasto.ALIMENTACAO);
        when(repository.findById(1L)).thenReturn(Optional.of(gasto));

        String resultado = gastoService.apagarGastoPorId(vinculo, 1L);

        assertThat(resultado).contains("Gasto apagado com sucesso");
        verify(repository).delete(gasto);
    }

    @Test
    void deveBloquearApagarGastoDeOutroUsuario() {
        TelegramVinculo vinculoOutro = TelegramVinculo.builder().id(99L).chatId("999").build();
        Gasto gasto = criarGasto(1L, "Mercado", BigDecimal.valueOf(200), CategoriaGasto.ALIMENTACAO);
        when(repository.findById(1L)).thenReturn(Optional.of(gasto));

        String resultado = gastoService.apagarGastoPorId(vinculoOutro, 1L);

        assertThat(resultado).contains("não autorizada");
        verify(repository, never()).delete(any());
    }

    @Test
    void deveRetornarMensagemGastoNaoEncontradoPorId() {
        TelegramVinculo vinculo = criarVinculo();
        when(repository.findById(99L)).thenReturn(Optional.empty());

        String resultado = gastoService.apagarGastoPorId(vinculo, 99L);

        assertThat(resultado).contains("Nenhum gasto encontrado com o ID informado");
    }

    @Test
    void deveEditarGastoComSucesso() {
        TelegramVinculo vinculo = criarVinculo();
        Gasto gasto = criarGasto(1L, "Padaria", BigDecimal.valueOf(25), CategoriaGasto.ALIMENTACAO);
        when(repository.findById(1L)).thenReturn(Optional.of(gasto));
        when(repository.save(any(Gasto.class))).thenReturn(gasto);

        String resultado = gastoService.editarGasto(vinculo, 1L, "Mercado", 50.0,
                CategoriaGasto.ALIMENTACAO, LocalDate.now(), "Itens novos");

        assertThat(resultado).contains("Gasto atualizado com sucesso");
        assertThat(gasto.getEstabelecimento()).isEqualTo("Mercado");
        assertThat(gasto.getValor()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
    }

    @Test
    void deveEditarGastoParcialmente() {
        TelegramVinculo vinculo = criarVinculo();
        Gasto gasto = criarGasto(1L, "Padaria", BigDecimal.valueOf(25), CategoriaGasto.ALIMENTACAO);
        when(repository.findById(1L)).thenReturn(Optional.of(gasto));
        when(repository.save(any(Gasto.class))).thenReturn(gasto);

        // Só altera o valor, demais ficam nulos/vazios
        String resultado = gastoService.editarGasto(vinculo, 1L, null, 50.0, null, null, null);

        assertThat(resultado).contains("Gasto atualizado");
        assertThat(gasto.getEstabelecimento()).isEqualTo("Padaria"); // mantém original
        assertThat(gasto.getValor()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
    }

    @Test
    void deveApagarDescricaoDoGasto() {
        TelegramVinculo vinculo = criarVinculo();
        Gasto gasto = criarGasto(1L, "Padaria", BigDecimal.valueOf(25), CategoriaGasto.ALIMENTACAO);
        gasto.setDescricao("Descrição antiga");
        when(repository.findById(1L)).thenReturn(Optional.of(gasto));
        when(repository.save(any(Gasto.class))).thenReturn(gasto);

        gastoService.editarGasto(vinculo, 1L, null, 0.0, null, null, "apagar_descricao");

        assertThat(gasto.getDescricao()).isNull();
    }

    @Test
    void deveBloquearEdicaoDeGastoDeOutroUsuario() {
        TelegramVinculo vinculoOutro = TelegramVinculo.builder().id(99L).chatId("999").build();
        Gasto gasto = criarGasto(1L, "Mercado", BigDecimal.valueOf(200), CategoriaGasto.ALIMENTACAO);
        when(repository.findById(1L)).thenReturn(Optional.of(gasto));

        String resultado = gastoService.editarGasto(vinculoOutro, 1L, "Novo", 100.0, null, null, null);

        assertThat(resultado).contains("não autorizada");
        verify(repository, never()).save(any());
    }
}
