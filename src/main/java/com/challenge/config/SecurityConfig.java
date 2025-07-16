package com.challenge.config; // Certifique-se de que o pacote está correto

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;


@Configuration
public class SecurityConfig {



    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {

        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector).servletPath("/api");

        http
                .csrf(csrf -> csrf
                                .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**"))
                                .ignoringRequestMatchers(mvcMatcherBuilder.pattern("/alugueis/upload"))
                        .ignoringRequestMatchers(mvcMatcherBuilder.pattern("/ws"))
                         )
                .authorizeHttpRequests(authorize -> authorize
                                .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern("/alugueis/upload")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern("/alugueis/**")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern("/carros/**")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern("/ws")).permitAll()
                                .anyRequest().authenticated()
                        // .anyRequest().permitAll()
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:4200"); // Exemplo direto
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}