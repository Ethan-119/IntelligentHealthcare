package com.intelligenthealthcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IntelligentHealthcareApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelligentHealthcareApplication.class, args);
    }
}
