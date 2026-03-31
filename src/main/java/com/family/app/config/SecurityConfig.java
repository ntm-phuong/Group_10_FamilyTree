package com.family.app.config;

import com.family.app.security.JwtAuthenticationFilter; // Import Filter của bạn
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    return config;
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Mở cửa hoàn toàn cho login/register

                        .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/news/**").hasRole("FAMILY_HEAD")
                        .requestMatchers(HttpMethod.PUT, "/api/news/**").hasRole("FAMILY_HEAD")
                        .requestMatchers(HttpMethod.DELETE, "/api/news/**").hasRole("FAMILY_HEAD")

                        .requestMatchers("/api/family-head/**").hasRole("FAMILY_HEAD")
                        .anyRequest().authenticated()
                        // Cho phép các tài nguyên tĩnh
                        .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()

                        // Cho phép các route của PageController (Các trang giao diện public)
                        .requestMatchers("/login", "/about", "/family-tree", "/news/**").permitAll()

                        // Cho phép API login/register
                        .requestMatchers("/api/auth/**").permitAll()

                        // Tất cả các request khác (thường là API nghiệp vụ) mới cần login
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}