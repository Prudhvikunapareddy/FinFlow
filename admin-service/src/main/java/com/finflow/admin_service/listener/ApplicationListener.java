package com.finflow.admin_service.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.finflow.admin_service.config.RabbitConfig;
import com.finflow.admin_service.dto.ApplicationMessageDTO;
import com.finflow.admin_service.repository.ApplicationRepository;

@Component
public class ApplicationListener {

    @Autowired
    private ApplicationRepository repository;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void receive(ApplicationMessageDTO message) {
        repository.upsertApplication(message.getId(), message.getName(), message.getStatus());
    }
}
