package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Usuario;
import com.tecsup.medicos_especialidades04.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// TODO (Angie): completar logica de validaciones, encriptar password, etc.
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public Usuario registrar(Usuario usuario) {
        // TODO (Angie): validar username unico, encriptar password con BCrypt
        return usuarioRepository.save(usuario);
    }

    public Usuario actualizar(Long id, Usuario usuario) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        existente.setNombres(usuario.getNombres());
        existente.setCorreo(usuario.getCorreo());
        existente.setEstado(usuario.getEstado());
        existente.setRol(usuario.getRol());
        if (usuario.getUsername() != null && !usuario.getUsername().isBlank()) {
            existente.setUsername(usuario.getUsername());
        }
        if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
            existente.setPassword(usuario.getPassword());
        }
        return usuarioRepository.save(existente);
    }

    public Usuario cambiarEstado(Long id, Boolean estado) {
        // Backbones para activar/desactivar
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        existente.setEstado(estado);
        return usuarioRepository.save(existente);
    }

    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }
}
