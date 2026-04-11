package com.family.app.config;

import com.family.app.security.AppPermissions;
import com.family.app.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
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
@EnableMethodSecurity
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
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    return config;
                }))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Cho phép các tài nguyên tĩnh
                        .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()

                        // Trang public (không gồm /family-head — cần quyền FAMILY_HEAD)
                        .requestMatchers("/login", "/about", "/family-tree", "/site-news-manage", "/news/**", "/member/**").permitAll()

                        // Trang dashboard quản trị họ: chỉ FAMILY_HEAD (+ JWT)
                        .requestMatchers("/family-head", "/family-head/**", "/admin", "/admin/**")
                                .hasAuthority(AppPermissions.FAMILY_HEAD)

                        // API thống kê dashboard: chỉ FAMILY_HEAD
                        .requestMatchers("/api/family-head/dashboard", "/api/family-head/dashboard/**")
                                .hasAuthority(AppPermissions.FAMILY_HEAD)

                        // API thành viên / chi / tin: đã đăng nhập — chi tiết quyền @PreAuthorize trên controller
                        .requestMatchers("/api/family-head/**").authenticated()

                        // Cho phép API login/register (trừ /me — cần JWT)
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/verify-email").permitAll()
                        .requestMatchers("/api/auth/active").permitAll()
                        .requestMatchers("/api/auth/forgot-password/**").permitAll()

                        .requestMatchers("/api/auth/me").authenticated()

                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}