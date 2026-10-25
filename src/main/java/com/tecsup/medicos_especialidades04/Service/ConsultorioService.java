package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Consultorio;
import com.tecsup.medicos_especialidades04.Model.Especialidades;
import com.tecsup.medicos_especialidades04.Model.EstadoConsultorio;
import com.tecsup.medicos_especialidades04.Model.HorarioAtencion;
import com.tecsup.medicos_especialidades04.Repository.ConsultorioRepository;
import com.tecsup.medicos_especialidades04.Repository.EspecialidadesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ConsultorioService {
    @Autowired private AlcanceMedicoService alcance;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @Autowired
    private EspecialidadesRepository especialidadesRepository;

    @Autowired
    private HorarioAtencionService horarioAtencionService;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired private com.tecsup.medicos_especialidades04.Repository.HorarioAtencionRepository horarios;

    public List<Consultorio> listar() {
        return alcance.esMedico() ? alcance.consultorios() : consultorioRepository.findAll();
    }

    public Consultorio obtener(Long id) {
        if (alcance.esMedico()) return alcance.consultorios().stream().filter(item -> item.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso no disponible para tu cuenta"));
        return consultorioRepository.findById(id).orElse(null);
    }

    @Transactional
    public Consultorio cambiarEstado(Long id, EstadoConsultorio estado) {
        if (estado == null || estado == EstadoConsultorio.OCUPADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado debe ser DISPONIBLE o INACTIVO; la ocupación se calcula por horario");
        }
        Consultorio consultorio = consultorioRepository.bloquearPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultorio no encontrado"));
        consultorio.setEstado(estado);
        Consultorio guardado = consultorioRepository.save(consultorio);
        auditoriaService.registrarOperacion("UPDATE", "Consultorio", id);
        return guardado;
    }

    @Transactional
    public Consultorio registrar(Consultorio consultorio) {
        validarDatos(consultorio);

        String codigoNormalizado = "TMP" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 17);

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

        if (consultorio.getEstado() == EstadoConsultorio.OCUPADO) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ocupación se calcula por horario; selecciona operativo o inactivo");
        Consultorio saved = consultorioRepository.saveAndFlush(consultorio);
        String base = "CON-" + String.format("%06d", saved.getId());
        String codigo = base;
        int sufijo = 1;
        while (consultorioRepository.existsByCodigoIgnoreCase(codigo)) codigo = base + "-" + sufijo++;
        saved.setCodigo(codigo);
        auditoriaService.registrarOperacion("INSERT", "Consultorio", saved.getId());
        return saved;
    }

    public HorarioAtencion asignarConsultorio(Long consultorioId, Long horarioId) {
        return horarioAtencionService.asignarConsultorio(consultorioId, horarioId);
    }

    @Transactional
    public Consultorio actualizar(Long id, Consultorio solicitud) {
        Consultorio existente = consultorioRepository.bloquearPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultorio no encontrado"));
        validarDatos(solicitud);
        Long anterior = existente.getEspecialidad() == null ? null : existente.getEspecialidad().getIdEspecialidad();
        Long nuevo = solicitud.getEspecialidad() == null ? null : solicitud.getEspecialidad().getIdEspecialidad();
        if (!java.util.Objects.equals(anterior, nuevo) && horarios.existsByConsultorio_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El consultorio tiene horarios: conserva su especialidad para mantener la coherencia del historial");
        }
        if (!java.util.Objects.equals(anterior, nuevo)) asociarEspecialidadExistente(solicitud);
        if (solicitud.getEstado() == null || solicitud.getEstado() == EstadoConsultorio.OCUPADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona operativo o inactivo");
        }
        existente.setNombre(solicitud.getNombre().trim());
        existente.setPiso(solicitud.getPiso());
        existente.setArea(solicitud.getArea().trim());
        existente.setEstado(solicitud.getEstado());
        if (!java.util.Objects.equals(anterior, nuevo)) existente.setEspecialidad(solicitud.getEspecialidad());
        auditoriaService.registrarOperacion("UPDATE", "Consultorio", id);
        return consultorioRepository.save(existente);
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
        if (!Boolean.TRUE.equals(especialidad.getEstado())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una especialidad activa");
    }

    private void validarDatos(Consultorio consultorio) {
        if (consultorio == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe enviar los datos del consultorio"
            );
        }

        if (consultorio.getNombre() == null || consultorio.getNombre().isBlank() || consultorio.getNombre().trim().length() > 100) {
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

        if (consultorio.getArea() == null || consultorio.getArea().isBlank() || consultorio.getArea().trim().length() > 100) {
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

        if (horarios.existsByConsultorio_Id(id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "El consultorio tiene horarios registrados. Inactívalo para conservar su historial");
        consultorioRepository.delete(consultorio);
        auditoriaService.registrarOperacion("DELETE", "Consultorio", id);
    }
}
