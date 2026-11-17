package br.com.stormaid.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INCIDENT_CREATED_QUEUE = "incident.created.queue";
    public static final String INCIDENT_AI_QUEUE = "incident.ai.queue";
    public static final String INCIDENT_STATUS_CHANGED_QUEUE = "incident.status.changed.queue";
    public static final String STATUS_EXCHANGE = "stormaid.events";
    public static final String STATUS_CHANGED_ROUTING_KEY = "incident.status.changed.v1";
    public static final String STATUS_DEAD_LETTER_EXCHANGE = "stormaid.status.dlx";
    public static final String STATUS_DEAD_LETTER_QUEUE = "stormaid.incident.status.changed.dlq";
    public static final String STATUS_DEAD_LETTER_ROUTING_KEY = "incident.status.dead";

    @Bean
    public Queue incidentCreatedQueue() {
        return new Queue(INCIDENT_CREATED_QUEUE, true);
    }

    @Bean
    public Queue incidentStatusChangedQueue() {
        return new Queue(INCIDENT_STATUS_CHANGED_QUEUE, true);
    }

    @Bean
    public DirectExchange stormaidEventsExchange() {
        return new DirectExchange(STATUS_EXCHANGE, true, false);
    }

    public static final String INCIDENT_AI_ROUTING_KEY = "incident.created.ai"; // ou a routing key usada no teu publisher

    @Bean
    public Binding incidentAiBinding(
            @Qualifier("incidentAiQueue") Queue queue,
            @Qualifier("stormaidEventsExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(INCIDENT_AI_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(
            ObjectMapper objectMapper
    ) {
        ObjectMapper rabbitObjectMapper = objectMapper.copy();

        rabbitObjectMapper.registerModule(new JavaTimeModule());

        rabbitObjectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        return new Jackson2JsonMessageConverter(
                rabbitObjectMapper
        );
    }


    @Bean
    public DirectExchange incidentStatusDeadLetterExchange() {
        return new DirectExchange(
                STATUS_DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue incidentStatusDeadLetterQueue() {
        return new Queue(
                STATUS_DEAD_LETTER_QUEUE,
                true
        );
    }

    @Bean
    public Binding incidentStatusDeadLetterBinding(
            @Qualifier("incidentStatusDeadLetterQueue") Queue queue,
            @Qualifier("incidentStatusDeadLetterExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(STATUS_DEAD_LETTER_ROUTING_KEY);
    }

    public static final String AI_DEAD_LETTER_EXCHANGE = "stormaid.ai.dlx";
    public static final String AI_DEAD_LETTER_QUEUE = "stormaid.incident.ai.dlq";
    public static final String AI_DEAD_LETTER_ROUTING_KEY = "incident.ai.dead";

    @Bean
    public Queue incidentAiQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(INCIDENT_AI_QUEUE)
                .withArgument("x-dead-letter-exchange", AI_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", AI_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange incidentAiDeadLetterExchange() {
        return new DirectExchange(AI_DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue incidentAiDeadLetterQueue() {
        return new Queue(AI_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding incidentAiDeadLetterBinding(
            @Qualifier("incidentAiDeadLetterQueue") Queue queue,
            @Qualifier("incidentAiDeadLetterExchange") DirectExchange exchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(AI_DEAD_LETTER_ROUTING_KEY);
    }

}
