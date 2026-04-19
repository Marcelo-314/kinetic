package com.chronicle.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.chronicle")
public class ChronicleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronicleApplication.class, args);
    }
}
