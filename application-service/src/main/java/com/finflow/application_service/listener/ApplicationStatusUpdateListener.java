package com.finflow.application_service.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.finflow.application_service.config.RabbitConfig;
import com.finflow.application_service.dto.ApplicationStatusUpdateDTO;
import com.finflow.application_service.entity.LoanApplication;
import com.finflow.application_service.repository.ApplicationRepository;

@Component
public class ApplicationStatusUpdateListener {

    private final ApplicationRepository repository;

    public ApplicationStatusUpdateListener(ApplicationRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = RabbitConfig.STATUS_UPDATE_QUEUE)
    public void receive(ApplicationStatusUpdateDTO message) {
        LoanApplication app = repository.findById(message.getId())
                .orElseThrow(() -> new RuntimeException("Application not found for status sync: " + message.getId()));

        app.setStatus(message.getStatus());
        repository.save(app);
    }
}
