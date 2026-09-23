package com.ticket.config;

import com.ticket.security.JwtValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityBeansConfig {

    @Bean
    public JwtValidator jwtValidator(@Value("${app.jwt.secret}") String jwtSecret) {
        return new JwtValidator(jwtSecret);
    }
}