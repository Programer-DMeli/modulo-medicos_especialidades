package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.MedicoEspecialidad;
import com.tecsup.medicos_especialidades04.Repository.MedicoEspecialidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicoEspecialidadService {

    @Autowired
    private MedicoEspecialidadRepository repo;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<MedicoEspecialidad> listar() {
        return repo.findAll();
    }

    public MedicoEspecialidad guardar(MedicoEspecialidad medicoEspecialidad) {
        MedicoEspecialidad saved = repo.save(medicoEspecialidad);
        auditoriaService.registrarOperacion(
            medicoEspecialidad.getIdMedicoEspecialidad() == null ? "CREATE" : "UPDATE",
            "MedicoEspecialidad",
            saved.getIdMedicoEspecialidad()
        );
        return saved;
    }

    public MedicoEspecialidad obtener(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
        auditoriaService.registrarOperacion("DELETE", "MedicoEspecialidad", id);
    }
}
