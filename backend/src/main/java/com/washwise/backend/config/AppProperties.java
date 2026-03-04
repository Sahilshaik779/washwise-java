package com.washwise.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter @Setter
public class AppProperties {
    private String frontendUrl = "http://localhost:5173";
    private Security security = new Security();
    private Cors cors = new Cors();

    @Getter @Setter
    public static class Security {
        private String jwtSecret;
        private int jwtExpirationMinutes = 30;
    }

    @Getter @Setter
    public static class Cors {
        private List<String> allowedOrigins;
    }
}