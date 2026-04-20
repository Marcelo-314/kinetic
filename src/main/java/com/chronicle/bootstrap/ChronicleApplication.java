package com.chronicle.bootstrap;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.chronicle")
@EntityScan(basePackages = "com.chronicle.adapters.out.persistence.entity")
@EnableJpaRepositories(basePackages = "com.chronicle.adapters.out.persistence.springdata")
@EnableScheduling
public class ChronicleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronicleApplication.class, args);
    }
}
