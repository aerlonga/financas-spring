package dev.financas.FinancasSpring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancasSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancasSpringApplication.class, args);
	}
}
