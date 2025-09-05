package com.willyes.clemenintegra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.willyes.clemenintegra")
@EnableScheduling
public class ClemenIntegraApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClemenIntegraApplication.class, args);
    }

}

