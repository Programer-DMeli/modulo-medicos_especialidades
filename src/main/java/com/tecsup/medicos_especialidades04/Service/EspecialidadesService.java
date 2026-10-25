package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Especialidades;
import com.tecsup.medicos_especialidades04.Repository.EspecialidadesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class EspecialidadesService {
    @Autowired private AlcanceMedicoService alcance;

    @Autowired
    private EspecialidadesRepository repo;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<Especialidades> listar() {
        return alcance.esMedico() ? alcance.especialidades() : repo.findAll();
    }

    public Especialidades guardar(Especialidades especialidad) {
        boolean nuevo = especialidad.getIdEspecialidad() == null;
        if (especialidad.getNombre() == null || especialidad.getNombre().isBlank() || especialidad.getNombre().trim().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la especialidad es obligatorio y admite hasta 100 caracteres");
        }
        especialidad.setNombre(especialidad.getNombre().trim());
        if (especialidad.getDescripcion() != null && especialidad.getDescripcion().length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción admite hasta 200 caracteres");
        }
        if (especialidad.getDuracionConsulta() == null || especialidad.getDuracionConsulta() <= 0 || especialidad.getDuracionConsulta() > 1440) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La duración de consulta debe estar entre 1 y 1440 minutos");
        }
        repo.findByNombreIgnoreCase(especialidad.getNombre())
                .filter(e -> !java.util.Objects.equals(e.getIdEspecialidad(), especialidad.getIdEspecialidad()))
                .ifPresent(e -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una especialidad con ese nombre"); });
        if (especialidad.getEstado() == null) especialidad.setEstado(true);
        especialidad.setCodigo(nuevo ? "TMP" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 17)
                : repo.findById(especialidad.getIdEspecialidad()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad no encontrada")).getCodigo());
        // Check for duplicate nombre (only when creating a new record)
        if (especialidad.getIdEspecialidad() == null) {
            if (repo.findByCodigo(especialidad.getCodigo()).isPresent()) {
                throw new IllegalArgumentException("Ya existe una especialidad con el código: " + especialidad.getCodigo());
            }
        }
        Especialidades saved = repo.saveAndFlush(especialidad);
        if (nuevo) {
            String base = "ESP-" + String.format("%06d", saved.getIdEspecialidad());
            String codigo = base;
            int sufijo = 1;
            while (repo.findByCodigo(codigo).isPresent()) codigo = base + "-" + sufijo++;
            saved.setCodigo(codigo);
        }
        auditoriaService.registrarOperacion(
            nuevo ? "INSERT" : "UPDATE",
            "Especialidad",
            saved.getIdEspecialidad()
        );
        return saved;
    }

    public Especialidades obtener(Long id) {
        if (alcance.esMedico()) return alcance.especialidades().stream().filter(item -> item.getIdEspecialidad().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso no disponible para tu cuenta"));
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

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad no encontrada");
    }

    public void eliminar(Long id) {
        Especialidades especialidad = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad no encontrada"));
        if (!especialidad.getConsultorios().isEmpty() || !especialidad.getMedicoEspecialidades().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La especialidad está asociada a médicos o consultorios. Inactívala para conservar las relaciones.");
        }
        repo.deleteById(id);
        auditoriaService.registrarOperacion("DELETE", "Especialidad", id);
    }
}
