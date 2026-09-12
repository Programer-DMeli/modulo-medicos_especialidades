package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Medico;
import com.tecsup.medicos_especialidades04.Repository.MedicoEspecialidadRepository;
import com.tecsup.medicos_especialidades04.Repository.MedicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicoService {

    @Autowired
    private MedicoRepository repo;

    @Autowired
    private MedicoEspecialidadRepository medicoEspecialidadRepo;

    public List<Medico> listar() {
        return repo.findAll();
    }

    public Medico guardar(Medico medico) {
        return repo.save(medico);
    }

    public Medico obtener(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}