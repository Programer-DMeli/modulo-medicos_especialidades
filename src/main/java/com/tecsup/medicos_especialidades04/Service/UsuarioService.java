package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Usuario;
import com.tecsup.medicos_especialidades04.Model.Rol;
import com.tecsup.medicos_especialidades04.Repository.RolRepository;
import com.tecsup.medicos_especialidades04.config.SessionRevocation;
import com.tecsup.medicos_especialidades04.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private SessionRevocation sessions;

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
        validarNombres(usuario);
        validarCuenta(usuario, true);
        if (usuarioRepository.existsByUsername(usuario.getUsername().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya existe");
        }
        usuario.setIdUsuario(null);
        usuario.setRol(resolverRolActivo(usuario.getRol()));
        usuario.setUsername(usuario.getUsername().trim());
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        Usuario saved = usuarioRepository.save(usuario);
        auditoriaService.registrarOperacion("INSERT", "Usuario", saved.getIdUsuario());
        return saved;
    }

    public Usuario actualizar(Long id, Usuario usuario) {
        validarNombres(usuario);
        validarCuenta(usuario, false);
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        String usernameAnterior = existente.getUsername();
        Rol nuevoRol = resolverRolActivo(usuario.getRol());
        if (existente.getMedico() != null && !"MEDICO".equals(nuevoRol.getNombre())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta vinculada a un médico debe conservar el rol MEDICO");
        }
        boolean nuevoEstado = usuario.getEstado() == null ? Boolean.TRUE.equals(existente.getEstado()) : usuario.getEstado();
        protegerUltimoAdministrador(existente, nuevoRol, nuevoEstado);
        existente.setNombres(usuario.getNombres());
        existente.setCorreo(usuario.getCorreo());
        if (usuario.getEstado() != null) existente.setEstado(usuario.getEstado());
        existente.setRol(nuevoRol);
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
        sessions.usuario(usernameAnterior);
        return saved;
    }

    public Usuario cambiarEstado(Long id, Boolean estado) {
        if (estado == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        }
        // Backbones para activar/desactivar
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        protegerUltimoAdministrador(existente, existente.getRol(), estado);
        existente.setEstado(estado);
        Usuario saved = usuarioRepository.save(existente);
        auditoriaService.registrarOperacion("UPDATE", "Usuario", saved.getIdUsuario());
        sessions.usuario(saved.getUsername());
        return saved;
    }

    public Usuario desactivar(Long id) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        protegerUltimoAdministrador(existente, existente.getRol(), false);
        existente.setEstado(false);
        Usuario saved = usuarioRepository.save(existente);
        auditoriaService.registrarOperacion("UPDATE", "Usuario", saved.getIdUsuario());
        sessions.usuario(saved.getUsername());
        return saved;
    }

    private Rol resolverRolActivo(Rol rol) {
        if (rol == null || rol.getIdRol() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccione un rol activo");
        }
        Rol existente = rolRepository.findById(rol.getIdRol())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
        if (!Boolean.TRUE.equals(existente.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede asignar un rol inactivo");
        }
        return existente;
    }

    private void validarNombres(Usuario usuario) {
        if (usuario == null || usuario.getNombres() == null || usuario.getNombres().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        usuario.setNombres(usuario.getNombres().trim());
        if (usuario.getNombres().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre admite hasta 100 caracteres");
        }
    }

    private void validarCuenta(Usuario usuario, boolean nueva) {
        if (nueva && (usuario.getUsername() == null || usuario.getUsername().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El username es obligatorio");
        }
        if (usuario.getUsername() != null && (usuario.getUsername().isBlank()
                || usuario.getUsername().trim().length() > 50)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El username debe contener entre 1 y 50 caracteres");
        }
        String correo = usuario.getCorreo();
        if (correo != null && !correo.isBlank()) {
            correo = correo.trim();
            if (correo.length() > 150 || !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingrese un correo válido de hasta 150 caracteres");
            }
            usuario.setCorreo(correo);
        } else usuario.setCorreo(null);
        String password = usuario.getPassword();
        if (nueva && (password == null || password.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
        }
        if (password != null && !password.isBlank()
                && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña admite hasta 72 bytes UTF-8");
        }
    }

    private void protegerUltimoAdministrador(Usuario actual, Rol nuevoRol, boolean nuevoEstado) {
        boolean pierdeAcceso = !nuevoEstado || nuevoRol == null || !"ADMINISTRADOR".equals(nuevoRol.getNombre());
        if (!pierdeAcceso || !Boolean.TRUE.equals(actual.getEstado()) || actual.getRol() == null
                || !"ADMINISTRADOR".equals(actual.getRol().getNombre())) return;
        // Serialize removals of administrator access, including concurrent changes.
        rolRepository.bloquearAdministrador();
        long activos = usuarioRepository.findAll().stream().filter(user -> Boolean.TRUE.equals(user.getEstado())
                && user.getRol() != null && "ADMINISTRADOR".equals(user.getRol().getNombre())
                && Boolean.TRUE.equals(user.getRol().getEstado())).count();
        if (activos <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe conservar al menos un administrador activo");
        }
    }
}
