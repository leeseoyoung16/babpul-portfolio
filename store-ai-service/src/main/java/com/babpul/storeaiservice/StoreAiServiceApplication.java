package com.babpul.storeaiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class StoreAiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StoreAiServiceApplication.class, args);
    }
}