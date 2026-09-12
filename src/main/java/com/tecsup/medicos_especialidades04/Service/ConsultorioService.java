package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Consultorio;
import com.tecsup.medicos_especialidades04.Model.Especialidades;
import com.tecsup.medicos_especialidades04.Model.EstadoConsultorio;
import com.tecsup.medicos_especialidades04.Model.HorarioAtencion;
import com.tecsup.medicos_especialidades04.Repository.ConsultorioRepository;
import com.tecsup.medicos_especialidades04.Repository.EspecialidadesRepository;
import com.tecsup.medicos_especialidades04.Repository.HorarioAtencionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ConsultorioService {

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @Autowired
    private EspecialidadesRepository especialidadesRepository;

    @Autowired
    private HorarioAtencionRepository horarioAtencionRepository;

    public List<Consultorio> listar() {
        return consultorioRepository.findAll();
    }

    public Consultorio obtener(Long id) {
        return consultorioRepository.findById(id).orElse(null);
    }

    @Transactional
    public Consultorio registrar(Consultorio consultorio) {
        validarDatos(consultorio);

        String codigoNormalizado = consultorio.getCodigo().trim().toUpperCase();

        if (consultorioRepository.existsByCodigoIgnoreCase(codigoNormalizado)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un consultorio con el código " + codigoNormalizado
            );
        }

        consultorio.setId(null);
        consultorio.setCodigo(codigoNormalizado);
        consultorio.setNombre(consultorio.getNombre().trim());
        consultorio.setArea(consultorio.getArea().trim());

        if (consultorio.getEstado() == null) {
            consultorio.setEstado(EstadoConsultorio.DISPONIBLE);
        }

        asociarEspecialidadExistente(consultorio);

        return consultorioRepository.save(consultorio);
    }

    @Transactional
    public HorarioAtencion asignarConsultorio(Long consultorioId, Long horarioId) {
        Consultorio consultorio = consultorioRepository.findById(consultorioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe el consultorio con ID " + consultorioId
                ));

        if (consultorio.getEstado() == EstadoConsultorio.INACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede asignar un consultorio inactivo"
            );
        }

        HorarioAtencion horario = horarioAtencionRepository.findById(horarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe el horario con ID " + horarioId
                ));

        if (horario.getMedico() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El horario no tiene un médico asociado"
            );
        }

        horario.setConsultorio(consultorio);

        return horarioAtencionRepository.save(horario);
    }

    private void asociarEspecialidadExistente(Consultorio consultorio) {
        if (consultorio.getEspecialidad() == null) {
            return;
        }

        Long especialidadId = consultorio.getEspecialidad().getIdEspecialidad();

        if (especialidadId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe enviar el ID de una especialidad existente"
            );
        }

        Especialidades especialidad = especialidadesRepository.findById(especialidadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe la especialidad con ID " + especialidadId
                ));

        consultorio.setEspecialidad(especialidad);
    }

    private void validarDatos(Consultorio consultorio) {
        if (consultorio == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe enviar los datos del consultorio"
            );
        }

        if (consultorio.getCodigo() == null || consultorio.getCodigo().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El código del consultorio es obligatorio"
            );
        }

        if (consultorio.getNombre() == null || consultorio.getNombre().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre del consultorio es obligatorio"
            );
        }

        if (consultorio.getPiso() == null || consultorio.getPiso() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El piso debe ser un número válido"
            );
        }

        if (consultorio.getArea() == null || consultorio.getArea().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El área del consultorio es obligatoria"
            );
        }
    }

    @Transactional
    public void eliminar(Long id) {
        Consultorio consultorio = consultorioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe el consultorio con ID " + id
                ));

        consultorioRepository.delete(consultorio);
    }
}
