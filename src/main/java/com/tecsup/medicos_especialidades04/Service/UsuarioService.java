package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Usuario;
import com.tecsup.medicos_especialidades04.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public Usuario registrar(Usuario usuario) {
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El username es obligatorio");
        }
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contrasena es obligatoria");
        }
        if (usuarioRepository.existsByUsername(usuario.getUsername().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya existe");
        }
        usuario.setUsername(usuario.getUsername().trim());
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        Usuario saved = usuarioRepository.save(usuario);
        auditoriaService.registrarOperacion("INSERT", "Usuario", saved.getIdUsuario());
        return saved;
    }

    public Usuario actualizar(Long id, Usuario usuario) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        existente.setNombres(usuario.getNombres());
        existente.setCorreo(usuario.getCorreo());
        existente.setEstado(usuario.getEstado());
        existente.setRol(usuario.getRol());
        if (usuario.getUsername() != null && !usuario.getUsername().isBlank()) {
            String nuevo = usuario.getUsername().trim();
            if (!nuevo.equals(existente.getUsername()) && usuarioRepository.existsByUsername(nuevo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya existe");
            }
            existente.setUsername(nuevo);
        }
        if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
            existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        Usuario saved = usuarioRepository.save(existente);
        auditoriaService.registrarOperacion("UPDATE", "Usuario", saved.getIdUsuario());
        return saved;
    }

    public Usuario cambiarEstado(Long id, Boolean estado) {
        // Backbones para activar/desactivar
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        existente.setEstado(estado);
        Usuario saved = usuarioRepository.save(existente);
        auditoriaService.registrarOperacion("UPDATE", "Usuario", saved.getIdUsuario());
        return saved;
    }

    public Usuario desactivar(Long id) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        existente.setEstado(false);
        Usuario saved = usuarioRepository.save(existente);
        auditoriaService.registrarOperacion("UPDATE", "Usuario", saved.getIdUsuario());
        return saved;
    }
}
