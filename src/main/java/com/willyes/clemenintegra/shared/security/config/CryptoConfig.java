package com.willyes.clemenintegra.shared.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class CryptoConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Ajusta la fuerza si lo necesitas (por defecto 10)
        return new BCryptPasswordEncoder(12);
    }
}

