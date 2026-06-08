package com.okabe.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            flyway.repair(); // Sửa các migration bị lỗi trước khi chạy migrate
            flyway.migrate(); // Thực hiện migration lên phiên bản mới nhất
        };
    }
}
