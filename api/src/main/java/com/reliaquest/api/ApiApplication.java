package com.reliaquest.api;

import com.reliaquest.api.config.EmployeeClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = "com.reliaquest.api")
@EnableRetry(proxyTargetClass = true)
@EnableConfigurationProperties(EmployeeClientProperties.class)
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
