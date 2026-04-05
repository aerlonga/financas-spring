package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.OrcamentoCategoria;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.repository.GastoRepository;
import dev.financas.FinancasSpring.repository.OrcamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final GastoRepository gastoRepository;

    public String consultarOrcamento(TelegramVinculo vinculo, CategoriaGasto categoria) {
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate hoje = LocalDate.now();

        if (categoria != null) {
            Optional<OrcamentoCategoria> orc = orcamentoRepository
                .findByTelegramVinculoAndCategoria(vinculo, categoria);

            BigDecimal gasto = gastoRepository.somarPorPeriodoECategoria(
                vinculo, inicioMes, hoje, categoria);

            if (orc.isEmpty()) {
                return String.format(Locale.forLanguageTag("pt-BR"), "Você não definiu um orçamento para %s. Gasto atual: R$ %.2f",
                    categoria.name(), gasto);
            }

            BigDecimal limite = orc.get().getValorLimite();
            BigDecimal saldo = limite.subtract(gasto);

            log.info("[Orcamento] Consulta por categoria: {} para chatId={}", categoria, vinculo.getChatId());
            return String.format(Locale.forLanguageTag("pt-BR"),
                "Orçamento %s:\nLimite: R$ %.2f\nGasto: R$ %.2f\nSaldo: R$ %.2f%s",
                categoria.name(), limite, gasto, saldo,
                saldo.compareTo(BigDecimal.ZERO) < 0 ? "\n⚠️ Orçamento estourado!" : ""
            );
        }


        // Visão geral de todas as categorias
        List<OrcamentoCategoria> todos = orcamentoRepository.findByTelegramVinculo(vinculo);
        BigDecimal totalGasto = gastoRepository.somarPorPeriodo(vinculo, inicioMes, hoje);

        if (todos.isEmpty()) {
            return String.format(Locale.forLanguageTag("pt-BR"), "Nenhum orçamento configurado. Gasto total este mês: R$ %.2f", totalGasto);
        }

        BigDecimal totalLimite = todos.stream()
            .map(OrcamentoCategoria::getValorLimite)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format(Locale.forLanguageTag("pt-BR"),
            "Resumo do mês:\nOrçamento total: R$ %.2f\nTotal gasto: R$ %.2f\nSaldo: R$ %.2f",
            totalLimite, totalGasto, totalLimite.subtract(totalGasto)
        );
    }

    @Transactional
    public String definirOrcamento(TelegramVinculo vinculo, CategoriaGasto categoria, BigDecimal limite) {
        OrcamentoCategoria orc = orcamentoRepository
            .findByTelegramVinculoAndCategoria(vinculo, categoria)
            .orElseGet(() -> OrcamentoCategoria.builder()
                .telegramVinculo(vinculo)
                .categoria(categoria)
                .build());

        orc.setValorLimite(limite);
        orcamentoRepository.save(orc);

        log.info("[Orcamento] Definindo limite de R$ {} para {} (chatId={})", limite, categoria, vinculo.getChatId());
        return String.format(Locale.forLanguageTag("pt-BR"), "Orçamento de R$ %.2f definido para %s.", limite, categoria.name());
    }
}
