package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Rol;
import com.tecsup.medicos_especialidades04.Repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// TODO (Angie): completar logica de validaciones y reglas de negocio
@Service
public class RolService {

    @Autowired
    private RolRepository rolRepository;

    public List<Rol> listar() {
        return rolRepository.findAll();
    }

    public Rol obtener(Long id) {
        return rolRepository.findById(id).orElse(null);
    }

    public Rol registrar(Rol rol) {
        // TODO (Angie): validar nombre unico y obligatorio
        return rolRepository.save(rol);
    }

    public Rol actualizar(Long id, Rol rol) {
        // TODO (Angie): completar actualizacion
        Rol existente = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
        existente.setNombre(rol.getNombre());
        existente.setDescripcion(rol.getDescripcion());
        return rolRepository.save(existente);
    }

    public void eliminar(Long id) {
        // TODO (Angie): validar que no tenga usuarios asociados antes de eliminar
        rolRepository.deleteById(id);
    }
}
