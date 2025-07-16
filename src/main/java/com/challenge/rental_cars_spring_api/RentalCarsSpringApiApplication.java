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

}
