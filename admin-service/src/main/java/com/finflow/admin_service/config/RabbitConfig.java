package com.finflow.admin_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Configuration
public class RabbitConfig {

    public static final String QUEUE = "application_queue";

    @Bean
    public Queue queue() {
        return new Queue(QUEUE);
    }

    //  VERY IMPORTANT (for object conversion)
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }
}