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
                        // 1. Tài nguyên tĩnh và Giao diện Public (Ưu tiên hàng đầu)
                        .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/login", "/about", "/family-tree", "/news/**").permitAll()

                        // 2. API xác thực (Login/Register)
                        .requestMatchers("/api/auth/**").permitAll()

                        // 3. API Tin tức (Public xem, Admin/Head mới được sửa)
                        .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/news/**").hasRole("FAMILY_HEAD")
                        .requestMatchers(HttpMethod.PUT, "/api/news/**").hasRole("FAMILY_HEAD")
                        .requestMatchers(HttpMethod.DELETE, "/api/news/**").hasRole("FAMILY_HEAD")

                        // 4. Các API quản lý gia đình
                        .requestMatchers("/api/family-head/**").hasRole("FAMILY_HEAD")

                        // 5. CHỐT CHẶN CUỐI CÙNG: Tất cả các request còn lại phải được xác thực
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}