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
import java.util.Locale;


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

    @Tool("Registra um gasto financeiro do usuário. USE APENAS se o usuário informou explicitamente o local, o valor e a categoria. JAMAIS invente esses dados se estiverem ausentes no contexto do usuário. Caso seja uma compra com vários itens, liste aqui na descrição.")
    public String registrarGasto(
        @P("Nome do estabelecimento ou local onde foi feita a compra") String estabelecimento,
        @P("Valor numérico do gasto em reais (ex: 49.90)") double valor,
        @P("Categoria: ALIMENTACAO, TRANSPORTE, SAUDE, MORADIA, LAZER, EDUCACAO ou OUTROS") String categoria,
        @P(value = "Data do gasto no formato dd/MM/yyyy. Se não informado, use data de hoje ou String vazia (NÃO use null).", required = false) String data,
        @P(value = "Descrição ou lista de itens comprados, caso fornecidos pelo usuário.", required = false) @Nullable String descricao
    ) {
        try {
            TelegramVinculo vinculo = getVinculo();
            CategoriaGasto cat = CategoriaGasto.valueOf(categoria.toUpperCase());
            LocalDate dataGasto = parseData(data);
            BigDecimal valorDecimal = BigDecimal.valueOf(valor);

            gastoService.registrar(vinculo, estabelecimento, valorDecimal, cat, dataGasto, descricao);

            String desc = (descricao != null && !descricao.isBlank()) ? "\n• Descrição: " + descricao : "";

            return String.format(Locale.forLanguageTag("pt-BR"),
                "✅ Gasto registrado!\n• Local: %s\n• Valor: R$ %.2f\n• Categoria: %s\n• Data: %s%s",
                estabelecimento, valor, cat.name(),
                dataGasto.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), desc
            );
        } catch (IllegalArgumentException e) {
            return "Categoria inválida. Use: ALIMENTACAO, TRANSPORTE, SAUDE, MORADIA, LAZER, EDUCACAO ou OUTROS.";
        } catch (Exception e) {
            return "Erro ao registrar o gasto: " + e.getMessage();
        }
    }

    @Tool("Consulta o resumo de gastos do usuário em um período específico. Aceita HOJE, ONTEM, SEMANA, MES, TOTAL, ou uma data específica (dd/MM/yyyy) ou intervalo de datas (dd/MM/yyyy-dd/MM/yyyy). Os resultados incluem o ID de cada gasto.")
    public String consultarGastos(
        @P("Período: HOJE, ONTEM, SEMANA, MES, TOTAL, data (ex: 01/04/2026) ou intervalo (ex: 01/04/2026-05/04/2026)") String periodo,
        @P(value = "Categoria específica (opcional). Ex: ALIMENTACAO. Use String vazia (\"\") ou omita para todos os gastos. NÃO use null.", required = false) @Nullable String categoria
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
        return String.format(Locale.forLanguageTag("pt-BR"), "✅ Economia de R$ %.2f registrada! (%s)", valor, descricao);
    }

    @Tool("Consulta o total economizado pelo usuário em um período específico. Aceita HOJE, SEMANA, MES, TOTAL, ou uma data específica (dd/MM/yyyy) ou intervalo de datas (dd/MM/yyyy-dd/MM/yyyy).")
    public String consultarEconomias(
        @P("Período: HOJE, SEMANA, MES, TOTAL, data (ex: 01/04/2026) ou intervalo (ex: 01/04/2026-05/04/2026)") String periodo
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
        @P(value = "Nome da categoria (ex: ALIMENTACAO). Use String vazia (\"\") ou omita para visão geral. NÃO use null.", required = false) @Nullable String categoria
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

    @Tool("Apaga o último gasto registrado pelo usuário. Útil quando o usuário diz que errou o valor/local e quer desfazer ou apagar o último registro.")
    public String apagarUltimoGasto() {
        TelegramVinculo vinculo = getVinculo();
        return gastoService.apagarUltimoGasto(vinculo);
    }

    @Tool("Apaga um gasto específico pelo seu ID. Use quando o usuário indicar qual registro deseja apagar após listar os gastos.")
    public String apagarGastoPorId(
        @P("ID do gasto que foi mostrado na listagem (ex: 45)") long id
    ) {
        TelegramVinculo vinculo = getVinculo();
        return gastoService.apagarGastoPorId(vinculo, id);
    }

    @Tool("Edita/altera um gasto existente pelo seu ID. Use quando o usuário quiser modificar o valor, local, categoria, data ou descrição de um registro já cadastrado. Informe apenas os campos que o usuário deseja alterar; deixe os demais como String vazia ou null.")
    public String editarGasto(
        @P("ID do gasto a ser editado (ex: 45)") long id,
        @P(value = "Novo nome do estabelecimento. Use String vazia (\"\") para não alterar.", required = false) @Nullable String novoEstabelecimento,
        @P(value = "Novo valor em reais. Use 0 para não alterar.", required = false) double novoValor,
        @P(value = "Nova categoria: ALIMENTACAO, TRANSPORTE, SAUDE, MORADIA, LAZER, EDUCACAO ou OUTROS. Use String vazia (\"\") para não alterar.", required = false) @Nullable String novaCategoria,
        @P(value = "Nova data no formato dd/MM/yyyy. Use String vazia (\"\") para não alterar.", required = false) @Nullable String novaData,
        @P(value = "Nova descrição ou itens ou 'apagar_descricao' para remover. Use String vazia para não alterar.", required = false) @Nullable String novaDescricao
    ) {
        TelegramVinculo vinculo = getVinculo();
        String estabelecimento = (novoEstabelecimento != null && !novoEstabelecimento.isBlank()) ? novoEstabelecimento : null;
        Double valor = (novoValor > 0) ? novoValor : null;
        CategoriaGasto cat = (novaCategoria != null && !novaCategoria.isBlank()) ? parseCategoriaSegura(novaCategoria) : null;
        LocalDate data = (novaData != null && !novaData.isBlank()) ? parseData(novaData) : null;
        return gastoService.editarGasto(vinculo, id, estabelecimento, valor, cat, data, novaDescricao);
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
        if (periodo == null || periodo.isBlank()) {
            return new LocalDate[]{LocalDate.of(2000, 1, 1), hoje};
        }
        return switch (periodo.toUpperCase().trim()) {
            case "HOJE"   -> new LocalDate[]{hoje, hoje};
            case "ONTEM"  -> new LocalDate[]{hoje.minusDays(1), hoje.minusDays(1)};
            case "SEMANA" -> new LocalDate[]{hoje.minusDays(7), hoje};
            case "MES"    -> new LocalDate[]{hoje.withDayOfMonth(1), hoje};
            case "TOTAL"  -> new LocalDate[]{LocalDate.of(2000, 1, 1), hoje};
            default       -> parseDatasEspecificas(periodo);
        };
    }

    private LocalDate[] parseDatasEspecificas(String periodo) {
        try {
            if (periodo.contains("-")) {
                String[] partes = periodo.split("-");
                return new LocalDate[]{
                    LocalDate.parse(partes[0].trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    LocalDate.parse(partes[1].trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                };
            }
            LocalDate data = LocalDate.parse(periodo.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return new LocalDate[]{data, data};
        } catch (Exception e) {
            return new LocalDate[]{LocalDate.of(2000, 1, 1), LocalDate.now()};
        }
    }

    private CategoriaGasto parseCategoriaSegura(String categoria) {
        try {
            return CategoriaGasto.valueOf(categoria.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
