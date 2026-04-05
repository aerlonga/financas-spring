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
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GastoService {

    private final GastoRepository repository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional
    public Gasto registrar(TelegramVinculo vinculo, String estabelecimento,
                           BigDecimal valor, CategoriaGasto categoria, LocalDate data, String descricao) {
        Gasto gasto = Gasto.builder()
            .telegramVinculo(vinculo)
            .estabelecimento(estabelecimento)
            .descricao(descricao)
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
        sb.append(String.format(Locale.forLanguageTag("pt-BR"), "Total gasto: R$ %.2f\n", total));
        sb.append(String.format("Registros: %d\n\n", gastos.size()));
        sb.append("Últimos lançamentos:\n");

        gastos.stream().limit(5).forEach(g -> {
            String desc = (g.getDescricao() != null && !g.getDescricao().isBlank()) ? "\n  Descrição: " + g.getDescricao() : "";
            sb.append(String.format(Locale.forLanguageTag("pt-BR"), "• %s: %s - R$ %.2f (%s) [ID: %d]%s\n",
                g.getDataGasto().format(FMT),
                g.getEstabelecimento(),
                g.getValor(),
                g.getCategoria().name(),
                g.getId(),
                desc));
        });

        if (gastos.size() > 5) {
            sb.append(String.format("...e mais %d lançamentos.", gastos.size() - 5));
        }

        return sb.toString();
    }

    @Transactional
    public String apagarUltimoGasto(TelegramVinculo vinculo) {
        return repository.findFirstByTelegramVinculoOrderByCriadoEmDesc(vinculo)
            .map(gasto -> {
                repository.delete(gasto);
                String desc = (gasto.getDescricao() != null && !gasto.getDescricao().isBlank()) ? "\n• Descrição: " + gasto.getDescricao() : "";
                return String.format(Locale.forLanguageTag("pt-BR"), "🗑️ Gasto apagado com sucesso!\n• Local: %s\n• Valor: R$ %.2f\n• Categoria: %s%s",
                    gasto.getEstabelecimento(), gasto.getValor(), gasto.getCategoria().name(), desc);
            })
            .orElse("Nenhum gasto encontrado para apagar.");
    }

    @Transactional
    public String apagarGastoPorId(TelegramVinculo vinculo, Long id) {
        return repository.findById(id)
            .map(gasto -> {
                if (!gasto.getTelegramVinculo().getId().equals(vinculo.getId())) {
                    return "🚫 Operação não autorizada. O gasto não pertence a você.";
                }
                repository.delete(gasto);
                String desc = (gasto.getDescricao() != null && !gasto.getDescricao().isBlank()) ? "\n• Descrição: " + gasto.getDescricao() : "";
                return String.format(Locale.forLanguageTag("pt-BR"), "🗑️ Gasto apagado com sucesso!\n• Local: %s\n• Valor: R$ %.2f\n• Categoria: %s%s",
                    gasto.getEstabelecimento(), gasto.getValor(), gasto.getCategoria().name(), desc);
            })
            .orElse("Nenhum gasto encontrado com o ID informado.");
    }

    @Transactional
    public String editarGasto(TelegramVinculo vinculo, Long id,
                              String novoEstabelecimento, Double novoValor,
                              CategoriaGasto novaCategoria, LocalDate novaData, String novaDescricao) {
        return repository.findById(id)
            .map(gasto -> {
                if (!gasto.getTelegramVinculo().getId().equals(vinculo.getId())) {
                    return "🚫 Operação não autorizada. O gasto não pertence a você.";
                }
                if (novoEstabelecimento != null && !novoEstabelecimento.isBlank()) {
                    gasto.setEstabelecimento(novoEstabelecimento);
                }
                if (novaDescricao != null && !novaDescricao.isBlank()) {
                    if (novaDescricao.equalsIgnoreCase("apagar_descricao")) {
                        gasto.setDescricao(null);
                    } else {
                        gasto.setDescricao(novaDescricao);
                    }
                }
                if (novoValor != null) {
                    gasto.setValor(BigDecimal.valueOf(novoValor));
                }
                if (novaCategoria != null) {
                    gasto.setCategoria(novaCategoria);
                }
                if (novaData != null) {
                    gasto.setDataGasto(novaData);
                }
                repository.save(gasto);
                String desc = (gasto.getDescricao() != null && !gasto.getDescricao().isBlank()) ? "\n• Descrição: " + gasto.getDescricao() : "";
                return String.format(Locale.forLanguageTag("pt-BR"), "✅ Gasto atualizado com sucesso!\n• Local: %s\n• Valor: R$ %.2f\n• Categoria: %s\n• Data: %s%s",
                    gasto.getEstabelecimento(), gasto.getValor(), gasto.getCategoria().name(),
                    gasto.getDataGasto().format(FMT), desc);
            })
            .orElse("Nenhum gasto encontrado com o ID informado.");
    }
}

