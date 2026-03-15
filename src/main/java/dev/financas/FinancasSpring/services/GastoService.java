package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.model.entities.Gasto;
import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.repository.GastoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GastoService {

    private final GastoRepository repository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional
    public Gasto registrar(TelegramVinculo vinculo, String estabelecimento,
                           BigDecimal valor, CategoriaGasto categoria, LocalDate data) {
        Gasto gasto = Gasto.builder()
            .telegramVinculo(vinculo)
            .estabelecimento(estabelecimento)
            .valor(valor)
            .categoria(categoria)
            .dataGasto(data != null ? data : LocalDate.now())
            .build();
        return repository.save(gasto);
    }

    public String consultarResumo(TelegramVinculo vinculo, LocalDate inicio,
                                   LocalDate fim, CategoriaGasto categoria) {
        List<Gasto> gastos;
        BigDecimal total;

        if (categoria != null) {
            gastos = repository.findByTelegramVinculoAndDataGastoBetweenAndCategoriaOrderByDataGastoDesc(
                vinculo, inicio, fim, categoria);
            total = repository.somarPorPeriodoECategoria(vinculo, inicio, fim, categoria);
        } else {
            gastos = repository.findByTelegramVinculoAndDataGastoBetweenOrderByDataGastoDesc(
                vinculo, inicio, fim);
            total = repository.somarPorPeriodo(vinculo, inicio, fim);
        }

        if (gastos.isEmpty()) {
            return "Nenhum gasto encontrado no período informado.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total gasto: R$ %.2f\n", total));
        sb.append(String.format("Registros: %d\n\n", gastos.size()));
        sb.append("Últimos lançamentos:\n");

        gastos.stream().limit(5).forEach(g ->
            sb.append(String.format("• %s: %s - R$ %.2f (%s)\n",
                g.getDataGasto().format(FMT),
                g.getEstabelecimento(),
                g.getValor(),
                g.getCategoria().name()))
        );

        if (gastos.size() > 5) {
            sb.append(String.format("...e mais %d lançamentos.", gastos.size() - 5));
        }

        return sb.toString();
    }
}
