package com.rebuy.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rebuy URL Shortener API")
                        .description(
                                "Production-grade URL shortener for Rebuy " +
                                        "product recommendation links. " +
                                        "Shortens long tracking URLs into clean " +
                                        "4-character links for SMS and email campaigns."
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Rebuy Engineering")
                                .email("engineering@rebuy.com")
                        )
                );
    }
}