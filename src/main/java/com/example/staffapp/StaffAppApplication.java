package com.example.staffapp;

import com.example.staffapp.model.Staff;
import org.apache.ibatis.type.MappedTypes;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@MappedTypes(Staff.class)
@MapperScan("com.example.staffapp.mapper")
@EnableAsync
@SpringBootApplication
public class StaffAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(StaffAppApplication.class, args);
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("StaffApp-");
        executor.initialize();
        return executor;
    }

}
