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

    public List<Especialidades> listar() {
        return repo.findAll();
    }

    public Especialidades guardar(Especialidades especialidad) {
        return repo.save(especialidad);
    }

    public Especialidades obtener(Long id) {
        return repo.findById(id).orElse(null);
    }

    public Especialidades cambiarEstado(Long id, Boolean estado) {
        Especialidades especialidad = repo.findById(id).orElse(null);

        if (especialidad != null) {
            especialidad.setEstado(estado);
            return repo.save(especialidad);
        }

        return null;
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}