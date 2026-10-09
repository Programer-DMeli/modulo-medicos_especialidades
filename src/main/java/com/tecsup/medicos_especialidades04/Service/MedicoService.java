package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Medico;
import com.tecsup.medicos_especialidades04.Repository.MedicoEspecialidadRepository;
import com.tecsup.medicos_especialidades04.Repository.MedicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class MedicoService {

    @Autowired
    private MedicoRepository repo;

    @Autowired
    private MedicoEspecialidadRepository medicoEspecialidadRepo;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<Medico> listar() {
        return repo.findAll();
    }

    public Medico guardar(Medico medico) {
        validarUnicidad(medico);

        Medico saved = repo.save(medico);
        auditoriaService.registrarOperacion(
            medico.getIdMedico() == null ? "INSERT" : "UPDATE",
            "Medico",
            saved.getIdMedico()
        );
        return saved;
    }

    /**
     * Evita el error duro de la base de datos (duplicate key) y devuelve
     * un 409 con un mensaje claro al panel de administracion.
     * Al editar, se ignora al propio medico que se esta modificando.
     */
    private void validarUnicidad(Medico medico) {
        if (medico.getNumeroDocumento() != null && !medico.getNumeroDocumento().isBlank()) {
            String documento = medico.getNumeroDocumento().trim();
            repo.findByNumeroDocumento(documento)
                .filter(existente -> !Objects.equals(existente.getIdMedico(), medico.getIdMedico()))
                .ifPresent(existente -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El numero de documento " + documento
                            + " ya esta registrado al medico " + existente.getNombres()
                            + " " + existente.getApellidoPaterno()
                            + " (codigo " + existente.getCodigoMedico() + ")");
                });
        }

        if (medico.getCodigoMedico() != null && !medico.getCodigoMedico().isBlank()) {
            String codigo = medico.getCodigoMedico().trim();
            repo.findByCodigoMedico(codigo)
                .filter(existente -> !Objects.equals(existente.getIdMedico(), medico.getIdMedico()))
                .ifPresent(existente -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El codigo de medico " + codigo + " ya esta registrado");
                });
        }
    }

    public Medico obtener(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
        auditoriaService.registrarOperacion("DELETE", "Medico", id);
    }
}
