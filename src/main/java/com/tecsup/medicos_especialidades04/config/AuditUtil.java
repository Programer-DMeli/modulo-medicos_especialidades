package com.tecsup.medicos_especialidades04.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class AuditUtil {

    /**
     * Devuelve el usuario autenticado en la sesion actual.
     * Si no hay sesion (o es anonima) retorna "SYSTEM".
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "SYSTEM";
        }
        if (auth.getPrincipal() instanceof UserDetails user) {
            return user.getUsername();
        }
        return auth.getName();
    }
}
