package com.washwise.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Allows NotificationService to send emails in the background
public class WashwiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(WashwiseApplication.class, args);
    }
}