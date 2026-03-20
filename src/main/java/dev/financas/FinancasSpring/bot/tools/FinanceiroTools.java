package dev.financas.FinancasSpring.bot.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.services.GastoService;
import dev.financas.FinancasSpring.services.EconomiaService;
import dev.financas.FinancasSpring.services.OrcamentoService;
import dev.financas.FinancasSpring.services.TelegramVinculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.lang.Nullable;


@Component
@RequiredArgsConstructor
public class FinanceiroTools {

    private final GastoService gastoService;
    private final EconomiaService economiaService;
    private final OrcamentoService orcamentoService;
    private final TelegramVinculoService vinculoService;

    private static final ThreadLocal<String> CHAT_ID_CONTEXT = new ThreadLocal<>();

    public static void setChatId(String chatId) {
        CHAT_ID_CONTEXT.set(chatId);
    }

    public static String getChatId() {
        return CHAT_ID_CONTEXT.get();
    }

    public static void clearChatId() {
        CHAT_ID_CONTEXT.remove();
    }

    private TelegramVinculo getVinculo() {
        String chatId = CHAT_ID_CONTEXT.get();
        if (chatId == null) throw new IllegalStateException("chatId não definido no contexto.");
        return vinculoService.obterOuCriar(chatId, "sistema");
    }

    @Tool("Registra um gasto financeiro do usuário. USE APENAS se o usuário informou explicitamente o local, o valor e a categoria. JAMAIS invente esses dados se estiverem ausentes no contexto do usuário.")
    public String registrarGasto(
        @P("Nome do estabelecimento ou local onde foi feita a compra") String estabelecimento,
        @P("Valor numérico do gasto em reais (ex: 49.90)") double valor,
        @P("Categoria: ALIMENTACAO, TRANSPORTE, SAUDE, MORADIA, LAZER, EDUCACAO ou OUTROS") String categoria,
        @P("Data do gasto no formato dd/MM/yyyy. Se não informado, usar data de hoje.") String data
    ) {
        try {
            TelegramVinculo vinculo = getVinculo();
            CategoriaGasto cat = CategoriaGasto.valueOf(categoria.toUpperCase());
            LocalDate dataGasto = parseData(data);
            BigDecimal valorDecimal = BigDecimal.valueOf(valor);

            gastoService.registrar(vinculo, estabelecimento, valorDecimal, cat, dataGasto);

            return String.format(
                "✅ Gasto registrado!\n• Local: %s\n• Valor: R$ %.2f\n• Categoria: %s\n• Data: %s",
                estabelecimento, valor, cat.name(),
                dataGasto.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );
        } catch (IllegalArgumentException e) {
            return "Categoria inválida. Use: ALIMENTACAO, TRANSPORTE, SAUDE, MORADIA, LAZER, EDUCACAO ou OUTROS.";
        } catch (Exception e) {
            return "Erro ao registrar o gasto: " + e.getMessage();
        }
    }

    @Tool("Consulta o resumo de gastos do usuário em um período. Use quando perguntar quanto gastou hoje, essa semana, esse mês ou em uma categoria específica.")
    public String consultarGastos(
        @P("Período: HOJE, ONTEM, SEMANA, MES ou TOTAL") String periodo,
        @P("Categoria específica (opcional). Ex: ALIMENTACAO") @Nullable String categoria
    ) {
        TelegramVinculo vinculo = getVinculo();
        LocalDate[] datas = resolverPeriodo(periodo);
        CategoriaGasto cat = categoria != null && !categoria.isBlank()
            ? parseCategoriaSegura(categoria) : null;
        return gastoService.consultarResumo(vinculo, datas[0], datas[1], cat);
    }

    @Tool("Registra um valor que o usuário guardou ou economizou.")
    public String registrarEconomia(
        @P("Valor guardado em reais") double valor,
        @P("Descrição ou motivo (ex: salário, poupança, mesada)") String descricao
    ) {
        TelegramVinculo vinculo = getVinculo();
        economiaService.registrar(vinculo, BigDecimal.valueOf(valor), descricao);
        return String.format("✅ Economia de R$ %.2f registrada! (%s)", valor, descricao);
    }

    @Tool("Consulta o total economizado pelo usuário em um período.")
    public String consultarEconomias(
        @P("Período: HOJE, SEMANA, MES ou TOTAL") String periodo
    ) {
        TelegramVinculo vinculo = getVinculo();
        if ("TOTAL".equalsIgnoreCase(periodo)) {
            return economiaService.consultarTotal(vinculo, null, null);
        }
        LocalDate[] datas = resolverPeriodo(periodo);
        return economiaService.consultarTotal(vinculo, datas[0], datas[1]);
    }

    @Tool("Consulta o orçamento mensal e saldo disponível do usuário. Pode ser geral ou por categoria específica.")
    public String consultarOrcamento(
        @P("Nome da categoria (ex: ALIMENTACAO). Use null ou vazio para visão geral.") @Nullable String categoria
    ) {
        TelegramVinculo vinculo = getVinculo();
        CategoriaGasto cat = (categoria != null && !categoria.isBlank())
            ? parseCategoriaSegura(categoria) : null;
        return orcamentoService.consultarOrcamento(vinculo, cat);
    }

    @Tool("Define ou atualiza o limite de orçamento mensal para uma categoria.")
    public String definirOrcamento(
        @P("Categoria: ALIMENTACAO, TRANSPORTE, SAUDE, MORADIA, LAZER, EDUCACAO ou OUTROS") String categoria,
        @P("Valor limite mensal em reais") double limite
    ) {
        TelegramVinculo vinculo = getVinculo();
        CategoriaGasto cat = CategoriaGasto.valueOf(categoria.toUpperCase());
        return orcamentoService.definirOrcamento(vinculo, cat, BigDecimal.valueOf(limite));
    }

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank() || data.equalsIgnoreCase("hoje")) return LocalDate.now();
        if (data.equalsIgnoreCase("ontem")) return LocalDate.now().minusDays(1);
        try {
            return LocalDate.parse(data, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private LocalDate[] resolverPeriodo(String periodo) {
        LocalDate hoje = LocalDate.now();
        return switch (periodo.toUpperCase()) {
            case "HOJE"   -> new LocalDate[]{hoje, hoje};
            case "ONTEM"  -> new LocalDate[]{hoje.minusDays(1), hoje.minusDays(1)};
            case "SEMANA" -> new LocalDate[]{hoje.minusDays(7), hoje};
            case "MES"    -> new LocalDate[]{hoje.withDayOfMonth(1), hoje};
            default       -> new LocalDate[]{LocalDate.of(2000, 1, 1), hoje};
        };
    }

    private CategoriaGasto parseCategoriaSegura(String categoria) {
        try {
            return CategoriaGasto.valueOf(categoria.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
