package com.logistics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 配置类
 * 用于 Springdoc OpenAPI 生成 Swagger UI 和 API 文档
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI logisticsOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("电商物流管理系统 API 文档")
                .description("基于 Spring Boot + Vue 3 的电商物流管理系统后端 API 接口文档")
                .version("1.0.0")
                .contact(new Contact()
                    .name("开发团队")
                    .url("http://localhost:8080"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")));
    }
}
