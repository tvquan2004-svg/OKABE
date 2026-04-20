package com.okabe;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class OkabeApplication {

    public static void main(String[] args) {
        // 1. Try to load .env from current directory
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        
        // 2. If essential key is missing, try parent directory
        if (dotenv.get("CLOUDINARY_CLOUD_NAME") == null) {
            dotenv = Dotenv.configure()
                    .directory("../")
                    .ignoreIfMissing()
                    .load();
        }

        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        SpringApplication.run(OkabeApplication.class, args);
    }
}
