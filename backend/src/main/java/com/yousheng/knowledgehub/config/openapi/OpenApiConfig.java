package com.yousheng.knowledgehub.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI knowledgeHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KnowledgeHub API")
                        .description("Personal Markdown knowledge base and publishing platform API")
                        .version("v1"));
    }
}