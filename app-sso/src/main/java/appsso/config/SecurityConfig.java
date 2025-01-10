package appsso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/signin", "/css/**", "/images/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2Login()
                .loginPage("/signin")
                .defaultSuccessUrl("/login")
                .successHandler((request, response, authentication) -> {
                    response.sendRedirect("/login");
                })
                .failureUrl("/signin")
                .and()
                .logout()
                .logoutSuccessUrl("/signin");

        
        return http.build();
    }
} 