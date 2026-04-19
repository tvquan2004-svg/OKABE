package com.okabe;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class OkabeApplication {

    public static void main(String[] args) {
        // Load .env from the root directory of the project (one level up from backend)
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("..") // Point to the root directory where .env is located
                    .ignoreIfMissing()
                    .load();
            
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
        } catch (Exception e) {
            // Log or ignore if .env is missing (e.g. in production)
        }

        SpringApplication.run(OkabeApplication.class, args);
    }
}
