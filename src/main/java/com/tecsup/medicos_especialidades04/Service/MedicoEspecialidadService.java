package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.MedicoEspecialidad;
import com.tecsup.medicos_especialidades04.Repository.MedicoEspecialidadRepository;
import com.tecsup.medicos_especialidades04.Repository.MedicoRepository;
import com.tecsup.medicos_especialidades04.Repository.EspecialidadesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class MedicoEspecialidadService {

    @Autowired
    private MedicoEspecialidadRepository repo;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private EspecialidadesRepository especialidadesRepository;

    @Autowired private com.tecsup.medicos_especialidades04.Repository.HorarioAtencionRepository horarios;

    public List<MedicoEspecialidad> listar() {
        return repo.findAll();
    }

    public MedicoEspecialidad guardar(MedicoEspecialidad medicoEspecialidad) {
        if (medicoEspecialidad.getMedico() == null || medicoEspecialidad.getMedico().getIdMedico() == null
                || medicoEspecialidad.getEspecialidad() == null || medicoEspecialidad.getEspecialidad().getIdEspecialidad() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un médico y una especialidad existentes");
        }
        medicoEspecialidad.setMedico(medicoRepository.bloquearPorId(medicoEspecialidad.getMedico().getIdMedico())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médico no encontrado")));
        medicoEspecialidad.setEspecialidad(especialidadesRepository.findById(medicoEspecialidad.getEspecialidad().getIdEspecialidad())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad no encontrada")));
        boolean nuevo = medicoEspecialidad.getIdMedicoEspecialidad() == null;
        if (!Boolean.TRUE.equals(medicoEspecialidad.getEspecialidad().getEstado())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una especialidad activa");
        if (medicoEspecialidad.getSubespecialidad() != null && medicoEspecialidad.getSubespecialidad().length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La subespecialidad admite hasta 100 caracteres");
        for (MedicoEspecialidad existente : repo.findByMedico_IdMedico(medicoEspecialidad.getMedico().getIdMedico())) {
            if (!java.util.Objects.equals(existente.getIdMedicoEspecialidad(), medicoEspecialidad.getIdMedicoEspecialidad())
                    && existente.getEspecialidad().getIdEspecialidad().equals(medicoEspecialidad.getEspecialidad().getIdEspecialidad())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La especialidad ya está asignada a este médico");
            }
        }
        if (!nuevo) {
            MedicoEspecialidad anterior = repo.findById(medicoEspecialidad.getIdMedicoEspecialidad())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada"));
            if (!anterior.getMedico().getIdMedico().equals(medicoEspecialidad.getMedico().getIdMedico())
                    || !anterior.getEspecialidad().getIdEspecialidad().equals(medicoEspecialidad.getEspecialidad().getIdEspecialidad())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Conserva la relación existente; utiliza la edición del médico para cambiar especialidades");
            }
        }
        MedicoEspecialidad saved = repo.save(medicoEspecialidad);
        auditoriaService.registrarOperacion(
            nuevo ? "INSERT" : "UPDATE",
            "MedicoEspecialidad",
            saved.getIdMedicoEspecialidad()
        );
        return saved;
    }

    public MedicoEspecialidad obtener(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void eliminar(Long id) {
        MedicoEspecialidad relacion = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada"));
        medicoRepository.bloquearPorId(relacion.getMedico().getIdMedico());
        if (horarios.existsByMedico_IdMedicoAndEspecialidad_IdEspecialidad(relacion.getMedico().getIdMedico(), relacion.getEspecialidad().getIdEspecialidad())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La asignación tiene horarios registrados y debe conservarse");
        }
        if (repo.findByMedico_IdMedico(relacion.getMedico().getIdMedico()).size() <= 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "El médico debe conservar al menos una especialidad");
        repo.deleteById(id);
        auditoriaService.registrarOperacion("DELETE", "MedicoEspecialidad", id);
    }
}
