package com.challenge.rental_cars_spring_api;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;

@SpringBootApplication()
@ComponentScan(basePackages = {"config", "com.challenge"})
public class RentalCarsSpringApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentalCarsSpringApiApplication.class, args);
    }


    @PostConstruct
    public void debugTempDir() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        System.out.println("=== TEMP DIR DEBUG ===");
        System.out.println("java.io.tmpdir: " + tmpDir);

        File dir = new File(tmpDir);
        System.out.println("Existe: " + dir.exists());
        System.out.println("É diretório: " + dir.isDirectory());
        System.out.println("Pode escrever: " + dir.canWrite());
        System.out.println("Pode ler: " + dir.canRead());
    }
}
