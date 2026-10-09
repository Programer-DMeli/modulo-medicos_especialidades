package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Especialidades;
import com.tecsup.medicos_especialidades04.Repository.EspecialidadesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EspecialidadesService {

    @Autowired
    private EspecialidadesRepository repo;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<Especialidades> listar() {
        return repo.findAll();
    }

    public Especialidades guardar(Especialidades especialidad) {
        // Check for duplicate nombre (only when creating a new record)
        if (especialidad.getIdEspecialidad() == null) {
            if (repo.findByNombre(especialidad.getNombre()).isPresent()) {
                throw new IllegalArgumentException("Ya existe una especialidad con el nombre: " + especialidad.getNombre());
            }
            if (repo.findByCodigo(especialidad.getCodigo()).isPresent()) {
                throw new IllegalArgumentException("Ya existe una especialidad con el código: " + especialidad.getCodigo());
            }
        }
        Especialidades saved = repo.save(especialidad);
        auditoriaService.registrarOperacion(
            especialidad.getIdEspecialidad() == null ? "INSERT" : "UPDATE",
            "Especialidad",
            saved.getIdEspecialidad()
        );
        return saved;
    }

    public Especialidades obtener(Long id) {
        return repo.findById(id).orElse(null);
    }

    public Especialidades cambiarEstado(Long id, Boolean estado) {
        Especialidades especialidad = repo.findById(id).orElse(null);

        if (especialidad != null) {
            especialidad.setEstado(estado);
            Especialidades saved = repo.save(especialidad);
            auditoriaService.registrarOperacion("UPDATE", "Especialidad", saved.getIdEspecialidad());
            return saved;
        }

        return null;
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
        auditoriaService.registrarOperacion("DELETE", "Especialidad", id);
    }
}