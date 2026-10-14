package com.tecsup.medicos_especialidades04.config;

import com.tecsup.medicos_especialidades04.Repository.UsuarioRepository;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/** Invalidates existing logins when account permissions or state change. */
@Component
public class SessionRevocation {
    private final SessionRegistry sessions;
    private final UsuarioRepository usuarios;

    public SessionRevocation(SessionRegistry sessions, UsuarioRepository usuarios) {
        this.sessions = sessions;
        this.usuarios = usuarios;
    }

    public void usuario(String username) {
        for (Object principal : sessions.getAllPrincipals()) {
            String name = principal instanceof UserDetails details ? details.getUsername() : principal.toString();
            if (username.equals(name)) sessions.getAllSessions(principal, false).forEach(session -> session.expireNow());
        }
    }

    public void rol(Long id) {
        usuarios.findAll().stream()
                .filter(user -> user.getRol() != null && id.equals(user.getRol().getIdRol()))
                .forEach(user -> usuario(user.getUsername()));
    }
}
