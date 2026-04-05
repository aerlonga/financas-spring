package dev.financas.FinancasSpring;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
@Disabled("Teste manual — requer GEMINI_API_KEY real. Executar com: mvn test -Dtest=AiTest -Dspring.profiles.active=local")
public class AiTest {

    @Test
    public void testGeminiFailure() throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null) {
            System.out.println("GEMINI_API_KEY is not set in environment.");
            // Try to load from application properties if needed or just skip
        }

        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey != null ? apiKey : "dummy")
                .modelName("gemini-2.5-flash")
                .build();

        List<ChatMessage> messages = new ArrayList<>();

        // 1. User
        messages.add(UserMessage.from("ah, eu gastei tambem no dia 18/03/2026 50 reais na padaria e 400 reais no mercado no dia 15/03/2026"));

        // 2. AiMessage (ToolCall)
        String jsonToolCall = "{\"toolExecutionRequests\":[{\"name\":\"registrarGasto\",\"arguments\":\"{\\\"valor\\\":50.0,\\\"estabelecimento\\\":\\\"Padaria\\\",\\\"data\\\":\\\"18/03/2026\\\",\\\"categoria\\\":\\\"ALIMENTACAO\\\"}\"},{\"name\":\"registrarGasto\",\"arguments\":\"{\\\"data\\\":\\\"15/03/2026\\\",\\\"categoria\\\":\\\"ALIMENTACAO\\\",\\\"valor\\\":400.0,\\\"estabelecimento\\\":\\\"Mercado\\\"}\"}],\"type\":\"AI\"}";
        ChatMessage tc = ChatMessageDeserializer.messageFromJson(jsonToolCall);
        messages.add(tc);

        // 3. ToolResult 1
        String jsonResult1 = "{\"toolName\":\"registrarGasto\",\"text\":\"✅ Gasto registrado!\\n• Local: Padaria\\n• Valor: R$ 50.00\\n• Categoria: ALIMENTACAO\\n• Data: 18/03/2026\",\"type\":\"TOOL_EXECUTION_RESULT\"}";
        ChatMessage r1 = ChatMessageDeserializer.messageFromJson(jsonResult1);
        messages.add(r1);

        // 4. ToolResult 2
        String jsonResult2 = "{\"toolName\":\"registrarGasto\",\"text\":\"✅ Gasto registrado!\\n• Local: Mercado\\n• Valor: R$ 400.00\\n• Categoria: ALIMENTACAO\\n• Data: 15/03/2026\",\"type\":\"TOOL_EXECUTION_RESULT\"}";
        ChatMessage r2 = ChatMessageDeserializer.messageFromJson(jsonResult2);
        messages.add(r2);

        // 5. AiMessage (Final)
        messages.add(AiMessage.from("Ótimo! Registrei seus gastos: R$ 50,00 na Padaria em 18/03/2026 e R$ 400,00 no Mercado em 15/03/2026, ambos na categoria ALIMENTAÇÃO."));

        // 6. User (New message)
        messages.add(UserMessage.from("quanto eu gastei nesse mes?"));

        System.out.println("--- Sending list of messages indices ---");
        for (int i = 0; i < messages.size(); i++) {
            System.out.println(i + ": " + messages.get(i).getClass().getSimpleName());
        }

        try {
            Response<AiMessage> response = model.generate(messages);
            System.out.println("Response: " + response.content().text());
        } catch (Exception e) {
            System.out.println("CAUGHT EX: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
