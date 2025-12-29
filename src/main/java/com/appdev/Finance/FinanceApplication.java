package com.appdev.Finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync; // Import @EnableAsync

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.appdev.Finance.Repository")
@EntityScan(basePackages = "com.appdev.Finance.model")
@EnableAsync // Add this annotation to enable asynchronous processing
public class FinanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}