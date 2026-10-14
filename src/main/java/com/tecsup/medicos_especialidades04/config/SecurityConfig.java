package com.tecsup.medicos_especialidades04.config;

import com.tecsup.medicos_especialidades04.Service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final String[] LECTORES = {"ADMINISTRADOR", "RECEPCIONISTA", "MEDICO"};
    private static final String[] PROGRAMADORES = {"ADMINISTRADOR", "RECEPCIONISTA"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessions) throws Exception {
        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1).sessionRegistry(sessions).maxSessionsPreventsLogin(false)
                .expiredSessionStrategy(event -> {
                    if (esApi(event.getRequest())) jsonError(event.getResponse(), 401, "Su sesión venció. Inicie sesión nuevamente");
                    else event.getResponse().sendRedirect(event.getRequest().getContextPath() + "/login?expired=true");
                }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/403", "/error", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/usuarios/**", "/roles/**", "/api/usuarios/**", "/api/roles/**", "/auditoria/**", "/api/auditoria/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/medicos/registrar").hasAnyRole("ADMINISTRADOR", "MEDICO")
                .requestMatchers("/especialidades/registrar", "/especialidades/estado").hasRole("ADMINISTRADOR")
                .requestMatchers("/consultorios/registrar", "/consultorios/asignar", "/horarios/registrar", "/horarios/editar/**").hasAnyRole(PROGRAMADORES)
                .requestMatchers(HttpMethod.GET, "/api/medicos/**", "/api/especialidades/**", "/api/medico/especialidades/**", "/api/consultorios/**", "/api/horarios/**").hasAnyRole(LECTORES)
                .requestMatchers("/api/medicos/**").hasAnyRole("ADMINISTRADOR", "MEDICO")
                .requestMatchers("/api/especialidades/**", "/api/medico/especialidades/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/consultorios/**", "/api/horarios/**").hasAnyRole(PROGRAMADORES)
                .requestMatchers("/api/**").denyAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
                .usernameParameter("username").passwordParameter("password")
                .defaultSuccessUrl("/", true).failureUrl("/login?error=true").permitAll())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, exception) -> {
                    if (esApi(request)) jsonError(response, 401, "Inicie sesión para continuar");
                    else response.sendRedirect(request.getContextPath() + "/login");
                })
                .accessDeniedHandler((request, response, exception) -> {
                    if (esApi(request)) jsonError(response, 403, "No tiene permiso o la sesión del formulario venció");
                    else response.sendRedirect(request.getContextPath() + "/403");
                }))
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true).deleteCookies("JSESSIONID").permitAll());
        // CSRF stays enabled: templates and fetch mutations must send the session token.
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(CustomUserDetailsService users, BCryptPasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    private static boolean esApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Bean
    public SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }

    @Bean
    public HttpSessionEventPublisher sessionEvents() { return new HttpSessionEventPublisher(); }

    private static void jsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
