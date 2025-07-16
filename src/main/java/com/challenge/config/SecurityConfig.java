package com.challenge.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins-front}")
    String frontEndUrl;

    public SecurityConfig() {
        System.out.println("🏗️ SecurityConfig CONSTRUTOR chamado!");
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("🔐 Criando SecurityFilterChain...");

        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .anyRequest().permitAll()
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        System.out.println("✅ SecurityFilterChain criado!");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        System.out.println("=================================");
        System.out.println("🔄 CRIANDO BEAN CORS");
        System.out.println("📡 Frontend URL: " + frontEndUrl);
        System.out.println("🕐 Timestamp: " + new java.util.Date());
        System.out.println("=================================");
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. Configure origem permitida
        configuration.setAllowedOrigins(Collections.singletonList(frontEndUrl));

        // 2. Métodos permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 3. Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 4. 🔥 CABEÇALHOS ESSENCIAIS QUE ESTAVAM FALTANDO:
        configuration.setAllowCredentials(true);
        configuration.addExposedHeader("Content-Type");
        configuration.addExposedHeader("Content-Disposition");

        // 5. Cache de CORS
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        System.out.println("✅ BEAN CORS CRIADO COM SUCESSO");
        return source;
    }

    // ✅ Adicione este bean para forçar alta prioridade no filtro CORS
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorsFilter(corsConfigurationSource()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }



    @Bean
    public OncePerRequestFilter corsHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {

                response.setHeader("Access-Control-Allow-Origin", frontEndUrl);
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("Access-Control-Expose-Headers", "Content-Type, Content-Disposition");

                filterChain.doFilter(request, response);
            }
        };
    }
}
