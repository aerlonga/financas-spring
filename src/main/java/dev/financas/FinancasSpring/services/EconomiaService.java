package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.model.entities.Economia;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.repository.EconomiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EconomiaService {

    private final EconomiaRepository repository;

    @Transactional
    public Economia registrar(TelegramVinculo vinculo, BigDecimal valor, String descricao) {
        Economia economia = Economia.builder()
            .telegramVinculo(vinculo)
            .valor(valor)
            .descricao(descricao)
            .dataEconomia(LocalDate.now())
            .build();
        return repository.save(economia);
    }

    public String consultarTotal(TelegramVinculo vinculo, LocalDate inicio, LocalDate fim) {
        BigDecimal total;
        String periodo;

        if (inicio == null || fim == null) {
            total = repository.somarTotal(vinculo);
            periodo = "todo o histórico";
        } else {
            total = repository.somarPorPeriodo(vinculo, inicio, fim);
            periodo = String.format("%s a %s", inicio, fim);
        }

        return String.format("Total economizado (%s): R$ %.2f", periodo, total);
    }
}
