package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Rol;
import com.tecsup.medicos_especialidades04.Repository.RolRepository;
import com.tecsup.medicos_especialidades04.Repository.UsuarioRepository;
import com.tecsup.medicos_especialidades04.config.SessionRevocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RolService {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private SessionRevocation sessions;

    public List<Rol> listar() {
        return rolRepository.findAll();
    }

    public Rol obtener(Long id) {
        return rolRepository.findById(id).orElse(null);
    }

    public Rol registrar(Rol rol) {
        validarNombre(rol);
        if (rolRepository.existsByNombre(rol.getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol ya existe");
        }
        rol.setIdRol(null);
        if (rol.getEstado() == null) rol.setEstado(true);
        Rol saved = rolRepository.save(rol);
        auditoriaService.registrarOperacion("INSERT", "Rol", saved.getIdRol());
        return saved;
    }

    public Rol actualizar(Long id, Rol rol) {
        validarNombre(rol);
        Rol existente = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
        if (!existente.getNombre().equals(rol.getNombre())
                && rolRepository.existsByNombre(rol.getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol ya existe");
        }
        if (esRolBase(existente.getNombre()) && !existente.getNombre().equals(rol.getNombre())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los roles del sistema no se pueden renombrar");
        }
        existente.setNombre(rol.getNombre());
        existente.setDescripcion(rol.getDescripcion());
        if (rol.getEstado() != null) {
            validarDesactivacion(existente, rol.getEstado());
            existente.setEstado(rol.getEstado());
        }
        Rol saved = rolRepository.save(existente);
        auditoriaService.registrarOperacion("UPDATE", "Rol", saved.getIdRol());
        sessions.rol(saved.getIdRol());
        return saved;
    }

    public void eliminar(Long id) {
        Rol existente = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
        if (esRolBase(existente.getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Desactive el rol del sistema en lugar de eliminarlo");
        }
        if (usuarioRepository.existeRolEnUso(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar: hay usuarios con este rol");
        }
        rolRepository.deleteById(id);
        auditoriaService.registrarOperacion("DELETE", "Rol", id);
    }

    public Rol cambiarEstado(Long id, Boolean activo) {
        if (activo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        }
        Rol existente = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
        validarDesactivacion(existente, activo);
        existente.setEstado(activo);
        Rol saved = rolRepository.save(existente);
        auditoriaService.registrarOperacion("UPDATE", "Rol", id);
        sessions.rol(id);
        return saved;
    }

    private void validarDesactivacion(Rol existente, Boolean activo) {
        if (!activo && "ADMINISTRADOR".equals(existente.getNombre())) {
            rolRepository.bloquearAdministrador();
            if (usuarioRepository.existeRolEnUso(existente.getIdRol())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede desactivar el rol administrador mientras tenga usuarios asignados");
            }
        }
    }

    private void validarNombre(Rol rol) {
        if (rol == null || rol.getNombre() == null || rol.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del rol es obligatorio");
        }
        rol.setNombre(rol.getNombre().trim().toUpperCase(java.util.Locale.ROOT));
        if (rol.getNombre().length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del rol admite hasta 50 caracteres");
        }
        if (rol.getDescripcion() != null && rol.getDescripcion().length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción admite hasta 200 caracteres");
        }
    }

    private boolean esRolBase(String nombre) {
        return java.util.Set.of("ADMINISTRADOR", "MEDICO", "RECEPCIONISTA").contains(nombre);
    }
}
