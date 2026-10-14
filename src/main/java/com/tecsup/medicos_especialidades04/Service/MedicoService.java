package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Medico;
import com.tecsup.medicos_especialidades04.Repository.MedicoEspecialidadRepository;
import com.tecsup.medicos_especialidades04.Repository.MedicoRepository;
import com.tecsup.medicos_especialidades04.Repository.AusenciaMedicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.LinkedHashSet;
import com.tecsup.medicos_especialidades04.Model.*;
import com.tecsup.medicos_especialidades04.Repository.EspecialidadesRepository;
import com.tecsup.medicos_especialidades04.Repository.HorarioAtencionRepository;

@Service
@Transactional
public class MedicoService {

    @Autowired
    private MedicoRepository repo;

    @Autowired
    private MedicoEspecialidadRepository medicoEspecialidadRepo;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private AusenciaMedicoRepository ausenciaRepository;

    @Autowired private EspecialidadesRepository especialidadesRepo;
    @Autowired private HorarioAtencionRepository horariosRepo;

    public List<Medico> listar() {
        return repo.findAll();
    }

    public Medico cambiarEstado(Long id, EstadoMedico estado) {
        if (estado == null || estado == EstadoMedico.VACACIONES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado debe ser ACTIVO o SUSPENDIDO; registra las vacaciones como ausencia");
        }
        Medico medico = repo.bloquearPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médico no encontrado"));
        medico.setEstado(estado);
        Medico guardado = repo.save(medico);
        auditoriaService.registrarOperacion("UPDATE", "Medico", id);
        return guardado;
    }

    public Medico guardar(Medico medico) {
        boolean nuevo = medico.getIdMedico() == null;
        Medico anterior = nuevo ? null : repo.bloquearPorId(medico.getIdMedico())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médico no encontrado"));
        String codigoAnterior = anterior == null ? null : anterior.getCodigoMedico();
        validarDatos(medico);
        validarUnicidad(medico);
        List<Long> seleccion = medico.getEspecialidadIds();
        if (seleccion == null && !nuevo) seleccion = medicoEspecialidadRepo.findByMedico_IdMedico(medico.getIdMedico())
                .stream().map(r -> r.getEspecialidad().getIdEspecialidad()).toList();
        if (seleccion == null || seleccion.isEmpty() || seleccion.stream().anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona al menos una especialidad para el médico");
        }
        if (new LinkedHashSet<>(seleccion).size() != seleccion.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No repitas una especialidad para el mismo médico");
        }
        List<MedicoEspecialidad> actuales = nuevo ? List.of() : medicoEspecialidadRepo.findByMedico_IdMedico(medico.getIdMedico());
        List<Especialidades> elegidas = seleccion.stream().map(id -> especialidadesRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad no encontrada"))).toList();
        for (Especialidades especialidad : elegidas) {
            boolean yaAsignada = actuales.stream().anyMatch(r -> r.getEspecialidad().getIdEspecialidad().equals(especialidad.getIdEspecialidad()));
            if (!Boolean.TRUE.equals(especialidad.getEstado()) && !yaAsignada) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden asignar especialidades activas");
            }
        }
        for (MedicoEspecialidad relacion : actuales) {
            if (!seleccion.contains(relacion.getEspecialidad().getIdEspecialidad())
                    && horariosRepo.existsByMedico_IdMedicoAndEspecialidad_IdEspecialidad(medico.getIdMedico(), relacion.getEspecialidad().getIdEspecialidad())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Conserva las especialidades que tienen horarios registrados");
            }
        }
        // El ID de la base evita que dos altas simultáneas reciban el mismo código.
        medico.setCodigoMedico(nuevo ? "TMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 17) : codigoAnterior);
        Medico saved = repo.saveAndFlush(medico);
        if (nuevo) {
            String base = "MED-" + String.format("%06d", saved.getIdMedico());
            String codigo = base;
            int sufijo = 1;
            while (repo.findByCodigoMedico(codigo).isPresent()) codigo = base + "-" + sufijo++;
            saved.setCodigoMedico(codigo);
        }
        for (MedicoEspecialidad relacion : actuales) {
            if (!seleccion.contains(relacion.getEspecialidad().getIdEspecialidad())) {
                medicoEspecialidadRepo.delete(relacion);
                saved.getMedicoEspecialidades().remove(relacion);
                auditoriaService.registrarOperacion("DELETE", "MedicoEspecialidad", relacion.getIdMedicoEspecialidad());
            }
        }
        for (Especialidades especialidad : elegidas) {
            if (actuales.stream().noneMatch(r -> r.getEspecialidad().getIdEspecialidad().equals(especialidad.getIdEspecialidad()))) {
                MedicoEspecialidad relacion = medicoEspecialidadRepo.save(new MedicoEspecialidad(saved, especialidad, null));
                saved.getMedicoEspecialidades().add(relacion);
                auditoriaService.registrarOperacion("INSERT", "MedicoEspecialidad", relacion.getIdMedicoEspecialidad());
            }
        }
        auditoriaService.registrarOperacion(
            nuevo ? "INSERT" : "UPDATE",
            "Medico",
            saved.getIdMedico()
        );
        return saved;
    }

    private void validarDatos(Medico medico) {
        medico.setNombres(texto(medico.getNombres(), "Los nombres", 100));
        medico.setApellidoPaterno(texto(medico.getApellidoPaterno(), "El apellido paterno", 100));
        medico.setApellidoMaterno(medico.getApellidoMaterno() == null ? "" : medico.getApellidoMaterno().trim());
        if (medico.getApellidoMaterno().length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El apellido materno admite hasta 100 caracteres");
        medico.setTipoDocumento(texto(medico.getTipoDocumento(), "El tipo de documento", 10).toUpperCase(java.util.Locale.ROOT));
        medico.setNumeroDocumento(texto(medico.getNumeroDocumento(), "El documento", 15).toUpperCase(java.util.Locale.ROOT));
        boolean documentoValido = switch (medico.getTipoDocumento()) {
            case "DNI" -> medico.getNumeroDocumento().matches("[0-9]{8}");
            case "CE", "PASAPORTE" -> medico.getNumeroDocumento().matches("[A-Z0-9]{1,15}");
            default -> false;
        };
        if (!documentoValido) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documento inválido: DNI de 8 dígitos; CE o pasaporte con letras y números, hasta 15 caracteres");
        medico.setCmp(texto(medico.getCmp(), "El CMP", 20).toUpperCase(java.util.Locale.ROOT));
        medico.setRne(medico.getRne() == null || medico.getRne().isBlank() ? null : texto(medico.getRne(), "El RNE", 20).toUpperCase(java.util.Locale.ROOT));
        if (medico.getTelefono() == null || !medico.getTelefono().matches("9[0-9]{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El celular es obligatorio: debe tener 9 dígitos y empezar por 9");
        }
        medico.setCorreo(medico.getCorreo() == null || medico.getCorreo().isBlank() ? null : medico.getCorreo().trim());
        if (medico.getCorreo() != null && (medico.getCorreo().length() > 150 || !medico.getCorreo().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa un correo válido");
        }
        if (medico.getFechaIngreso() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de ingreso es obligatoria");
        if (medico.getEstado() == null) medico.setEstado(EstadoMedico.ACTIVO);
        if (medico.getEstado() == EstadoMedico.VACACIONES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registra las vacaciones como una ausencia con fechas");
    }

    private String texto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank() || valor.trim().length() > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " es obligatorio y admite hasta " + maximo + " caracteres");
        }
        return valor.trim();
    }

    /**
     * Evita el error duro de la base de datos (duplicate key) y devuelve
     * un 409 con un mensaje claro al panel de administracion.
     * Al editar, se ignora al propio medico que se esta modificando.
     */
    private void validarUnicidad(Medico medico) {
        repo.findByCmp(medico.getCmp()).filter(m -> !Objects.equals(m.getIdMedico(), medico.getIdMedico()))
                .ifPresent(m -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "El CMP ya pertenece a otro médico"); });
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

        if (medico.getIdMedico() != null && medico.getCodigoMedico() != null && !medico.getCodigoMedico().isBlank()) {
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
        Medico medico = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médico no encontrado"));
        if (!medico.getHorarios().isEmpty() || ausenciaRepository.existsByMedico_IdMedico(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este médico tiene horarios o ausencias registrados. Conserva su historial en lugar de eliminarlo.");
        }
        for (MedicoEspecialidad relacion : medicoEspecialidadRepo.findByMedico_IdMedico(id)) {
            medicoEspecialidadRepo.delete(relacion);
            auditoriaService.registrarOperacion("DELETE", "MedicoEspecialidad", relacion.getIdMedicoEspecialidad());
        }
        repo.deleteById(id);
        auditoriaService.registrarOperacion("DELETE", "Medico", id);
    }
}
