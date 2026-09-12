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

    // LISTAR TODOS LOS HORARIOS
    public List<HorarioAtencion> listar() {
        return repo.findAll();
    }

    // REGISTRAR HORARIO
    public HorarioAtencion guardar(HorarioAtencion horario) {
        return repo.save(horario);
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

        return repo.save(existente);
    }
    // eliminar
    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}