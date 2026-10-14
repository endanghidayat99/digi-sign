package com.digisign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DigiSignApplication {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(DigiSignApplication.class, args);
    }
}
