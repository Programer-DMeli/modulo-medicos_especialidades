package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Rol;
import com.tecsup.medicos_especialidades04.Repository.RolRepository;
import com.tecsup.medicos_especialidades04.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RolService {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<Rol> listar() {
        return rolRepository.findAll();
    }

    public Rol obtener(Long id) {
        return rolRepository.findById(id).orElse(null);
    }

    public Rol registrar(Rol rol) {
        if (rol.getNombre() == null || rol.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del rol es obligatorio");
        }
        if (rolRepository.existsByNombre(rol.getNombre().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol ya existe");
        }
        rol.setNombre(rol.getNombre().trim());
        Rol saved = rolRepository.save(rol);
        auditoriaService.registrarOperacion("INSERT", "Rol", saved.getIdRol());
        return saved;
    }

    public Rol actualizar(Long id, Rol rol) {
        Rol existente = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
        existente.setNombre(rol.getNombre());
        existente.setDescripcion(rol.getDescripcion());
        Rol saved = rolRepository.save(existente);
        auditoriaService.registrarOperacion("UPDATE", "Rol", saved.getIdRol());
        return saved;
    }

    public void eliminar(Long id) {
        if (usuarioRepository.existeRolEnUso(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar: hay usuarios con este rol");
        }
        rolRepository.deleteById(id);
        auditoriaService.registrarOperacion("DELETE", "Rol", id);
    }
}
