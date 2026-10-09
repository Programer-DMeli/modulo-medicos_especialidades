package com.tecsup.medicos_especialidades04.config;

import com.tecsup.medicos_especialidades04.Service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// TODO (Meliton - Pregunta 5): control de acceso por rol con Spring Security
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // Usuarios, roles y auditoria: solo administrador
                .requestMatchers("/usuarios/**", "/roles/**", "/api/usuarios/**",
                        "/api/roles/**", "/auditoria/**", "/api/auditoria/**")
                    .hasRole("ADMINISTRADOR")
                // Listado de medicos disponible para cualquier usuario autenticado (selects de otros modulos)
                .requestMatchers(HttpMethod.GET, "/api/medicos/**").authenticated()
                // Medicos: administrador o medico
                .requestMatchers("/medicos/**", "/api/medicos/**").hasAnyRole("ADMINISTRADOR", "MEDICO")
                // Horarios y consultorios: administrador o recepcionista
                .requestMatchers("/horarios/**", "/consultorios/**", "/api/horarios/**", "/api/consultorios/**")
                    .hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    response.sendRedirect("/");
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/403")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        org.springframework.security.authentication.dao.DaoAuthenticationProvider provider =
            new org.springframework.security.authentication.dao.DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}