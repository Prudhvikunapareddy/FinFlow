package com.finflow.admin_service.listener;



import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.finflow.admin_service.entity.Application;
import com.finflow.admin_service.config.RabbitConfig;
import com.finflow.admin_service.repository.ApplicationRepository;

@Component
public class ApplicationListener {

    @Autowired
    private ApplicationRepository repository;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void receive(Application app) {
        System.out.println(" Received Application:");
        System.out.println("Name: " + app.getName());
        System.out.println("Status: " + app.getStatus());
        
        repository.save(app);
        System.out.println("Application synced to Admin Database.");
    }
}