package com.finflow.application_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE = "application_queue";
    public static final String STATUS_UPDATE_QUEUE = "application_status_update_queue";

    @Bean
    public Queue statusUpdateQueue() {
        return new Queue(STATUS_UPDATE_QUEUE);
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }
}
