package dev.financas.FinancasSpring.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ para processamento de mensagens do Telegram.
 * <p>
 * Arquitetura:
 * - Exchange: telegram.exchange (direct)
 * - Queue principal: telegram.messages (fila de processamento)
 * - Dead Letter Queue: telegram.messages.dlq (mensagens com erro após retries)
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "telegram.exchange";
    public static final String QUEUE = "telegram.messages";
    public static final String ROUTING_KEY = "telegram.message.process";
    public static final String DLQ = "telegram.messages.dlq";
    public static final String DLX = "telegram.exchange.dlx";
    public static final String DLQ_ROUTING_KEY = "telegram.message.dead";

    @Value("${app.rabbitmq.consumers.min:3}")
    private int minConsumers;

    @Value("${app.rabbitmq.consumers.max:5}")
    private int maxConsumers;

    // ── Conversor JSON ──

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ── Dead Letter Exchange e Queue (para mensagens que falharam) ──

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DLQ_ROUTING_KEY);
    }

    // ── Exchange e Queue principal ──

    @Bean
    public DirectExchange telegramExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue telegramQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding telegramBinding() {
        return BindingBuilder.bind(telegramQueue())
                .to(telegramExchange())
                .with(ROUTING_KEY);
    }

    // ── Template e Factory ──

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        // Processa 1 mensagem por vez para controle de fluxo
        factory.setPrefetchCount(1);
        // Número de consumidores concorrentes (configurável via properties)
        factory.setConcurrentConsumers(minConsumers);
        factory.setMaxConcurrentConsumers(maxConsumers);
        // Acknowledge manual para garantir que a mensagem só saia da fila após processada
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}
