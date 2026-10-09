package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.HorarioAtencion;
import com.tecsup.medicos_especialidades04.Repository.HorarioAtencionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HorarioAtencionService {
    @Autowired
    private HorarioAtencionRepository repo;

    @Autowired
    private AuditoriaService auditoriaService;

    // LISTAR TODOS LOS HORARIOS
    public List<HorarioAtencion> listar() {
        return repo.findAll();
    }

    // REGISTRAR HORARIO
    public HorarioAtencion guardar(HorarioAtencion horario) {
        HorarioAtencion saved = repo.save(horario);
        auditoriaService.registrarOperacion(
            horario.getIdHorario() == null ? "INSERT" : "UPDATE",
            "HorarioAtencion",
            saved.getIdHorario()
        );
        return saved;
    }

    // BUSCAR HORARIO POR ID
    public HorarioAtencion obtener(Long id) {
        return repo.findById(id).orElse(null);
    }

    // ACTUALIZAR
    public HorarioAtencion actualizar(Long id, HorarioAtencion horario) {

        HorarioAtencion existente = obtener(id);

        if (existente == null) {
            return null;
        }
        existente.setMedico(horario.getMedico());
        existente.setConsultorio(horario.getConsultorio());
        existente.setDiaSemana(horario.getDiaSemana());
        existente.setHoraInicio(horario.getHoraInicio());
        existente.setHoraFin(horario.getHoraFin());
        existente.setDuracionCita(horario.getDuracionCita());
        existente.setEstado(horario.getEstado());

        HorarioAtencion saved = repo.save(existente);
        auditoriaService.registrarOperacion("UPDATE", "HorarioAtencion", saved.getIdHorario());
        return saved;
    }
    // eliminar
    public void eliminar(Long id) {
        repo.deleteById(id);
        auditoriaService.registrarOperacion("DELETE", "HorarioAtencion", id);
    }
}