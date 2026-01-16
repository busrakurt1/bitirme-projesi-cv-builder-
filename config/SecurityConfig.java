package com.cvbuilder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {}) // varsayılan CORS (gerekirse detaylandırırız)
                .authorizeHttpRequests(auth -> auth
                        // 🔓 Giriş / kayıt / health tamamen serbest
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/health",
                                "/api/auth/test"
                        ).permitAll()
                        // Geri kalan her şeye da şimdilik izin ver
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
