package ru.pavel2107.interfax.starter;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SparkClient {

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            InputDirectoryScanner scanner = InputDirectoryScanner.getInstance();
            scanner.start();
        };
    }

    public static void main(String[] args){
        SpringApplication.run( SparkClient.class, args);
    }
}
