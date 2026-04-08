package com.trimurl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrimUrlApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrimUrlApplication.class, args);
    }
}
