package com.mailvault.attachmentworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AttachmentWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttachmentWorkerApplication.class, args);
    }
}
