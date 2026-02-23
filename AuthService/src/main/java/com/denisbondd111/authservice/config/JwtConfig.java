package com.denisbondd111.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secret;
    private Duration accessTokenExpiration;
    private Duration refreshTokenExpiration;
}
