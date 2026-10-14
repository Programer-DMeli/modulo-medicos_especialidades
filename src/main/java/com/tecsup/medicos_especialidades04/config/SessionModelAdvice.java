package com.tecsup.medicos_especialidades04.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SessionModelAdvice {
    @ModelAttribute
    public void usuarioActual(Authentication authentication, Model model) {
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        String role = authenticated ? authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .findFirst().orElse("") : "";
        boolean admin = "ADMINISTRADOR".equals(role);
        model.addAttribute("currentUser", authenticated ? authentication.getName() : "");
        model.addAttribute("currentRole", role);
        model.addAttribute("esAdministrador", admin);
        model.addAttribute("puedeProgramar", admin || "RECEPCIONISTA".equals(role));
    }
}
