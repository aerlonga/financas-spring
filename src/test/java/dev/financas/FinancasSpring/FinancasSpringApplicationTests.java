package dev.financas.FinancasSpring;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requer beans complexos (AI, RabbitMQ, Telegram). Os testes unitários e @WebMvcTest cobrem a lógica de negócio.")
class FinancasSpringApplicationTests {

	@Test
	void contextLoads() {
	}

}
