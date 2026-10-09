package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Usuario;
import com.tecsup.medicos_especialidades04.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

// TODO (Meliton - pregunta 5): carga usuarios desde la BD para el login
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (Boolean.FALSE.equals(u.getEstado())) {
            throw new UsernameNotFoundException("Usuario desactivado: " + username);
        }
        java.util.List<SimpleGrantedAuthority> authorities = u.getRol() == null
                ? Collections.emptyList()
                : Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + u.getRol().getNombre()));
        return new User(u.getUsername(), u.getPassword(), authorities);
    }
}
