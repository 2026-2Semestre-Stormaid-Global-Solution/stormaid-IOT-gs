
package br.com.stormaid.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INCIDENT_CREATED_QUEUE = "incident.created.queue";
    public static final String INCIDENT_AI_QUEUE = "incident.ai.queue";
    public static final String INCIDENT_STATUS_CHANGED_QUEUE = "incident.status.changed.queue";

    @Bean
    public Queue incidentCreatedQueue() {
        return new Queue(INCIDENT_CREATED_QUEUE, true);
    }

    @Bean
    public Queue incidentAiQueue() {
        return new Queue(INCIDENT_AI_QUEUE, true);
    }

    @Bean
    public Queue incidentStatusChangedQueue() {
        return new Queue(INCIDENT_STATUS_CHANGED_QUEUE, true);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
