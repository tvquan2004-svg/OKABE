package com.okabe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(); // Tạo executor để chạy các tác vụ bất đồng bộ
        executor.setCorePoolSize(2); // Số luồng tối thiểu trong pool
        executor.setMaxPoolSize(5); // Số luồng tối đa trong pool
        executor.setQueueCapacity(200); // Dung lượng hàng đợi khi tất cả luồng đều bận
        executor.setThreadNamePrefix("async-"); // Tiền tố đặt tên cho các luồng async
        executor.setWaitForTasksToCompleteOnShutdown(true); // Chờ tác vụ đang chạy hoàn thành trước khi tắt
        executor.setAwaitTerminationSeconds(30); // Thời gian tối đa chờ tác vụ hoàn thành khi tắt
        executor.initialize(); // Khởi tạo executor sau khi cấu hình xong
        return executor;
    }
}
