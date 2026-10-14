package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.*;
import com.tecsup.medicos_especialidades04.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class HorarioAtencionService {
    private static final ZoneId ZONA_CLINICA = ZoneId.of("America/Lima");
    private static final List<String> DIAS = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");

    @Autowired private HorarioAtencionRepository repo;
    @Autowired private AusenciaMedicoRepository ausenciaRepo;
    @Autowired private MedicoRepository medicoRepo;
    @Autowired private ConsultorioRepository consultorioRepo;
    @Autowired private EspecialidadesRepository especialidadesRepo;
    @Autowired private MedicoEspecialidadRepository asignacionesRepo;
    @Autowired private AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<HorarioAtencion> listar() { return repo.findAll(); }

    @Transactional(readOnly = true)
    public HorarioAtencion obtener(Long id) { return repo.findById(id).orElse(null); }

    // READ_COMMITTED evita leer una instantánea anterior a la espera de los bloqueos en MySQL.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HorarioAtencion guardar(HorarioAtencion horario) {
        if (horario == null || horario.getIdHorario() != null) {
            throw error(HttpStatus.BAD_REQUEST, "Un horario nuevo no debe incluir idHorario");
        }
        validarDatos(horario);
        validarInicioNuevo(horario);
        bloquearRecursos(horario, null);
        validarCruces(horario, null);
        HorarioAtencion saved = repo.save(horario);
        auditoriaService.registrarOperacion("INSERT", "HorarioAtencion", saved.getIdHorario());
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HorarioAtencion actualizar(Long id, HorarioAtencion horario) {
        HorarioAtencion existente = repo.bloquearPorId(id).orElse(null);
        if (existente == null) return null;
        if (horario == null || (horario.getIdHorario() != null && !id.equals(horario.getIdHorario()))) {
            throw error(HttpStatus.BAD_REQUEST, "El identificador del horario no coincide con la ruta");
        }
        return actualizarBloqueado(existente, horario);
    }

    // También la utiliza ConsultorioService: ninguna ruta evita las reglas.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public HorarioAtencion asignarConsultorio(Long consultorioId, Long horarioId) {
        HorarioAtencion existente = repo.bloquearPorId(horarioId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "El horario no existe"));
        if (existente.getVigenteDesde() == null) {
            throw error(HttpStatus.CONFLICT, "Completa la vigencia del horario heredado antes de asignar consultorio");
        }
        HorarioAtencion solicitud = copiar(existente);
        Consultorio referencia = new Consultorio();
        referencia.setId(consultorioId);
        solicitud.setConsultorio(referencia);
        if (!existente.getVigenteDesde().isAfter(hoy())) solicitud.setVigenteDesde(hoy().plusDays(1));
        return actualizarBloqueado(existente, solicitud);
    }

    private HorarioAtencion actualizarBloqueado(HorarioAtencion existente, HorarioAtencion solicitud) {
        validarDatos(solicitud);
        validarInicioNuevo(solicitud);
        if (existente.getVigenteHasta() != null && existente.getVigenteHasta().isBefore(hoy())) {
            throw error(HttpStatus.CONFLICT, "El horario terminó; crea uno nuevo para conservar su historial");
        }
        boolean iniciado = existente.getVigenteDesde() != null && !existente.getVigenteDesde().isAfter(hoy());
        if (iniciado && !solicitud.getVigenteDesde().isAfter(hoy())) {
            throw error(HttpStatus.CONFLICT, "Para modificar un horario iniciado, elige una vigencia desde mañana; se creará una versión nueva");
        }
        bloquearRecursos(solicitud, existente);
        validarCruces(solicitud, existente.getIdHorario());
        if (iniciado) {
            LocalDate cierre = solicitud.getVigenteDesde().minusDays(1);
            if (existente.getVigenteHasta() == null || cierre.isBefore(existente.getVigenteHasta())) {
                existente.setVigenteHasta(cierre);
            }
            repo.save(existente);
            auditoriaService.registrarOperacion("UPDATE", "HorarioAtencion", existente.getIdHorario());
            HorarioAtencion saved = repo.save(copiar(solicitud));
            auditoriaService.registrarOperacion("INSERT", "HorarioAtencion", saved.getIdHorario());
            return saved;
        }
        copiarDatos(solicitud, existente);
        HorarioAtencion saved = repo.save(existente);
        auditoriaService.registrarOperacion("UPDATE", "HorarioAtencion", saved.getIdHorario());
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void eliminar(Long id) {
        HorarioAtencion horario = repo.bloquearPorId(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "El horario no existe"));
        HorarioAtencion recursos = copiar(horario);
        recursos.setEstado("INACTIVO");
        bloquearRecursos(recursos, null);
        if (horario.getVigenteDesde() != null && !horario.getVigenteDesde().isAfter(hoy())) {
            if (horario.getVigenteHasta() != null && !horario.getVigenteHasta().isAfter(hoy())) {
                throw error(HttpStatus.CONFLICT, "El horario ya terminó o está cerrado; se conserva su historial");
            }
            horario.setVigenteHasta(hoy());
            repo.save(horario);
            auditoriaService.registrarOperacion("UPDATE", "HorarioAtencion", id);
        } else {
            repo.delete(horario);
            auditoriaService.registrarOperacion("DELETE", "HorarioAtencion", id);
        }
    }

    private void validarDatos(HorarioAtencion horario) {
        if (horario.getDuracionCita() == null && horario.getEspecialidad() != null && horario.getEspecialidad().getIdEspecialidad() != null) {
            especialidadesRepo.findById(horario.getEspecialidad().getIdEspecialidad())
                    .ifPresent(e -> horario.setDuracionCita(e.getDuracionConsulta()));
        }
        horario.setDiaSemana(normalizarDia(horario.getDiaSemana()));
        horario.setEstado(normalizarEstado(horario.getEstado()));
        if (horario.getHoraInicio() == null || horario.getHoraFin() == null
                || !horario.getHoraInicio().isBefore(horario.getHoraFin())) {
            throw error(HttpStatus.BAD_REQUEST, "La hora de fin debe ser posterior al inicio dentro del mismo día");
        }
        if (horario.getDuracionCita() == null || horario.getDuracionCita() <= 0
                || horario.getDuracionCita() > Duration.between(horario.getHoraInicio(), horario.getHoraFin()).toMinutes()) {
            throw error(HttpStatus.BAD_REQUEST, "La duración de cita debe ser positiva y caber en el bloque");
        }
        if (horario.getVigenteDesde() == null) {
            throw error(HttpStatus.BAD_REQUEST, "La fecha de inicio de vigencia es obligatoria");
        }
        if (horario.getVigenteHasta() != null && horario.getVigenteHasta().isBefore(horario.getVigenteDesde())) {
            throw error(HttpStatus.BAD_REQUEST, "La vigencia final no puede ser anterior a la inicial");
        }
        if (horario.getVigenteHasta() != null && primeraOcurrencia(horario.getVigenteDesde(), horario.getDiaSemana())
                .isAfter(horario.getVigenteHasta())) {
            throw error(HttpStatus.BAD_REQUEST, "La vigencia debe incluir al menos un día del horario semanal");
        }
    }

    private void validarInicioNuevo(HorarioAtencion horario) {
        if (horario.getVigenteDesde().isBefore(hoy())) {
            throw error(HttpStatus.BAD_REQUEST, "La nueva vigencia debe comenzar hoy o en una fecha futura");
        }
    }

    private void bloquearRecursos(HorarioAtencion solicitud, HorarioAtencion anterior) {
        Long medicoId = idMedico(solicitud.getMedico());
        Long consultorioId = idConsultorio(solicitud.getConsultorio());
        if (medicoId == null || medicoId <= 0 || (solicitud.getConsultorio() != null && (consultorioId == null || consultorioId <= 0))) {
            throw error(HttpStatus.BAD_REQUEST, "Envía referencias válidas al médico y al consultorio");
        }
        // Primero médicos ordenados y luego consultorios ordenados en todos los casos.
        SortedSet<Long> medicos = new TreeSet<>();
        SortedSet<Long> consultorios = new TreeSet<>();
        medicos.add(medicoId);
        if (consultorioId != null) consultorios.add(consultorioId);
        if (anterior != null) {
            medicos.add(idMedico(anterior.getMedico()));
            if (anterior.getConsultorio() != null) consultorios.add(idConsultorio(anterior.getConsultorio()));
        }
        Medico medico = null;
        Consultorio consultorio = null;
        for (Long id : medicos) {
            Medico locked = medicoRepo.bloquearPorId(id)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "El médico indicado no existe"));
            if (id.equals(medicoId)) medico = locked;
        }
        for (Long id : consultorios) {
            Consultorio locked = consultorioRepo.bloquearPorId(id)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "El consultorio indicado no existe"));
            if (id.equals(consultorioId)) consultorio = locked;
        }
        if ("ACTIVO".equals(solicitud.getEstado()) && medico.getEstado() != EstadoMedico.ACTIVO) {
            throw error(HttpStatus.BAD_REQUEST, "Solo se pueden programar horarios activos para médicos activos");
        }
        if ("ACTIVO".equals(solicitud.getEstado()) && consultorio != null && consultorio.getEstado() == EstadoConsultorio.INACTIVO) {
            throw error(HttpStatus.BAD_REQUEST, "No se puede programar un consultorio inactivo");
        }
        if ("ACTIVO".equals(solicitud.getEstado())) {
            if (medico.getFechaIngreso() != null && solicitud.getVigenteDesde().isBefore(medico.getFechaIngreso())) {
                throw error(HttpStatus.BAD_REQUEST, "El horario no puede comenzar antes de la fecha de ingreso del médico");
            }
            if (solicitud.getEspecialidad() == null || solicitud.getEspecialidad().getIdEspecialidad() == null) {
                throw error(HttpStatus.BAD_REQUEST, "Selecciona la especialidad que el médico atenderá en esta franja");
            }
            Especialidades especialidad = especialidadesRepo.findById(solicitud.getEspecialidad().getIdEspecialidad())
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "La especialidad no existe"));
            if (!Boolean.TRUE.equals(especialidad.getEstado())) throw error(HttpStatus.BAD_REQUEST, "No se puede programar una especialidad inactiva");
            if (!asignacionesRepo.existsByMedico_IdMedicoAndEspecialidad_IdEspecialidad(medicoId, especialidad.getIdEspecialidad())) {
                throw error(HttpStatus.BAD_REQUEST, "La especialidad no está asignada al médico");
            }
            if (consultorio != null && consultorio.getEspecialidad() != null
                    && !consultorio.getEspecialidad().getIdEspecialidad().equals(especialidad.getIdEspecialidad())) {
                throw error(HttpStatus.BAD_REQUEST, "El consultorio no corresponde a la especialidad de esta franja");
            }
            solicitud.setEspecialidad(especialidad);
        }
        solicitud.setMedico(medico);
        solicitud.setConsultorio(consultorio);
    }

    private void validarCruces(HorarioAtencion nuevo, Long excluirId) {
        if (!"ACTIVO".equals(nuevo.getEstado())) return;
        for (HorarioAtencion otro : repo.buscarPorRecursos(idMedico(nuevo.getMedico()), idConsultorio(nuevo.getConsultorio()))) {
            if (Objects.equals(otro.getIdHorario(), excluirId)) continue;
            if (!normalizarDia(otro.getDiaSemana()).equals(nuevo.getDiaSemana())) continue;
            if (!nuevo.getHoraInicio().isBefore(otro.getHoraFin()) || !nuevo.getHoraFin().isAfter(otro.getHoraInicio())) continue;
            LocalDate desde = nuevo.getVigenteDesde().isAfter(otro.getVigenteDesde()) ? nuevo.getVigenteDesde() : otro.getVigenteDesde();
            LocalDate hasta = menorFin(nuevo.getVigenteHasta(), otro.getVigenteHasta());
            if (hasta != null && primeraOcurrencia(desde, nuevo.getDiaSemana()).isAfter(hasta)) continue;
            boolean mismoMedico = idMedico(nuevo.getMedico()).equals(idMedico(otro.getMedico()));
            throw error(HttpStatus.CONFLICT, mismoMedico
                    ? "El médico ya tiene un horario que se cruza en ese día y vigencia"
                    : "El consultorio ya está asignado a otro horario en ese día y vigencia");
        }
    }

    @Transactional(readOnly = true)
    public List<AusenciaMedico> listarAusencias() { return ausenciaRepo.listarConMedico(); }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AusenciaMedico guardarAusencia(AusenciaMedico ausencia) {
        if (ausencia == null || ausencia.getIdAusencia() != null) {
            throw error(HttpStatus.BAD_REQUEST, "Una ausencia nueva no debe incluir idAusencia");
        }
        ausencia.setEstado(normalizarEstado(ausencia.getEstado()));
        if (ausencia.getInicio() == null || ausencia.getFin() == null || !ausencia.getInicio().isBefore(ausencia.getFin())) {
            throw error(HttpStatus.BAD_REQUEST, "El fin de la ausencia debe ser posterior al inicio");
        }
        if (ausencia.getInicio().toLocalDate().isBefore(hoy())) {
            throw error(HttpStatus.BAD_REQUEST, "Una ausencia nueva debe comenzar hoy o en el futuro");
        }
        if (ausencia.getMotivo() == null || ausencia.getMotivo().isBlank() || ausencia.getMotivo().trim().length() > 200) {
            throw error(HttpStatus.BAD_REQUEST, "El motivo es obligatorio y admite hasta 200 caracteres");
        }
        Long medicoId = idMedico(ausencia.getMedico());
        if (medicoId == null || medicoId <= 0) throw error(HttpStatus.BAD_REQUEST, "Selecciona un médico existente");
        Medico medico = medicoRepo.bloquearPorId(medicoId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "El médico indicado no existe"));
        ausencia.setMedico(medico);
        ausencia.setMotivo(ausencia.getMotivo().trim());
        if ("ACTIVO".equals(ausencia.getEstado()) && ausenciaRepo.buscarEnRango(ausencia.getInicio(), ausencia.getFin())
                .stream().anyMatch(a -> medicoId.equals(idMedico(a.getMedico())))) {
            throw error(HttpStatus.CONFLICT, "El médico ya tiene una ausencia que se cruza con ese intervalo");
        }
        AusenciaMedico saved = ausenciaRepo.save(ausencia);
        auditoriaService.registrarOperacion("INSERT", "AusenciaMedico", saved.getIdAusencia());
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void eliminarAusencia(Long id) {
        AusenciaMedico ausencia = ausenciaRepo.bloquearPorId(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "La ausencia no existe"));
        medicoRepo.bloquearPorId(idMedico(ausencia.getMedico()));
        if (!"ACTIVO".equals(ausencia.getEstado())) throw error(HttpStatus.CONFLICT, "La ausencia ya está anulada");
        LocalDateTime ahora = LocalDateTime.now(ZONA_CLINICA);
        if (!ausencia.getFin().isAfter(ahora)) throw error(HttpStatus.CONFLICT, "La ausencia terminó y se conserva su historial");
        if (ausencia.getInicio().isAfter(ahora)) ausencia.setEstado("INACTIVO");
        else ausencia.setFin(ahora);
        ausenciaRepo.save(ausencia);
        auditoriaService.registrarOperacion("UPDATE", "AusenciaMedico", id);
    }

    public record MedicoAgenda(Long idMedico, String nombres, String apellidoPaterno, String apellidoMaterno, String codigoMedico) {}
    public record ConsultorioAgenda(Long id, String codigo, String nombre) {}
    public record EspecialidadAgenda(Long idEspecialidad, String nombre) {}
    public record BloqueAgenda(Long idHorario, MedicoAgenda medico, ConsultorioAgenda consultorio, LocalDate fecha,
                               LocalTime horaInicio, LocalTime horaFin, Integer duracionCita, String tipo, String motivo, EspecialidadAgenda especialidad) {}

    @Transactional(readOnly = true)
    public List<BloqueAgenda> agenda(LocalDate desde, LocalDate hasta, Long medicoId, Long consultorioId) {
        if (desde == null || hasta == null || hasta.isBefore(desde) || ChronoUnit.DAYS.between(desde, hasta) >= 31) {
            throw error(HttpStatus.BAD_REQUEST, "Elige un rango válido de hasta 31 días, incluyendo inicio y fin");
        }
        if (medicoId != null && !medicoRepo.existsById(medicoId)) throw error(HttpStatus.NOT_FOUND, "El médico no existe");
        if (consultorioId != null && !consultorioRepo.existsById(consultorioId)) throw error(HttpStatus.NOT_FOUND, "El consultorio no existe");
        List<AusenciaMedico> ausencias = ausenciaRepo.buscarEnRango(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        List<BloqueAgenda> bloques = new ArrayList<>();
        for (HorarioAtencion horario : repo.buscarParaAgenda(desde, hasta)) {
            if (medicoId != null && !medicoId.equals(idMedico(horario.getMedico()))) continue;
            if (consultorioId != null && !consultorioId.equals(idConsultorio(horario.getConsultorio()))) continue;
            LocalDate inicio = desde.isAfter(horario.getVigenteDesde()) ? desde : horario.getVigenteDesde();
            LocalDate fin = horario.getVigenteHasta() == null || hasta.isBefore(horario.getVigenteHasta()) ? hasta : horario.getVigenteHasta();
            for (LocalDate fecha = primeraOcurrencia(inicio, horario.getDiaSemana()); !fecha.isAfter(fin); fecha = fecha.plusWeeks(1)) {
                if (!fecha.isBefore(hoy())) {
                    if (horario.getMedico().getEstado() != EstadoMedico.ACTIVO) continue;
                    if (horario.getConsultorio() != null && horario.getConsultorio().getEstado() == EstadoConsultorio.INACTIVO) continue;
                    if (horario.getEspecialidad() != null && !Boolean.TRUE.equals(horario.getEspecialidad().getEstado())) continue;
                }
                agregarBloques(bloques, horario, fecha, ausencias);
            }
        }
        bloques.sort(Comparator.comparing(BloqueAgenda::fecha).thenComparing(BloqueAgenda::horaInicio)
                .thenComparing(b -> b.medico().idMedico()));
        return bloques;
    }

    private void agregarBloques(List<BloqueAgenda> resultado, HorarioAtencion horario, LocalDate fecha, List<AusenciaMedico> ausencias) {
        LocalDateTime inicio = fecha.atTime(horario.getHoraInicio());
        LocalDateTime fin = fecha.atTime(horario.getHoraFin());
        List<AusenciaMedico> relevantes = ausencias.stream()
                .filter(a -> idMedico(a.getMedico()).equals(idMedico(horario.getMedico())) && a.getInicio().isBefore(fin) && a.getFin().isAfter(inicio))
                .sorted(Comparator.comparing(AusenciaMedico::getInicio)).toList();
        LocalDateTime cursor = inicio;
        String tipo = horario.getConsultorio() == null || horario.getEspecialidad() == null ? "PENDIENTE" : "ATENCION";
        for (AusenciaMedico ausencia : relevantes) {
            LocalDateTime inicioAusencia = ausencia.getInicio().isAfter(cursor) ? ausencia.getInicio() : cursor;
            LocalDateTime finAusencia = ausencia.getFin().isBefore(fin) ? ausencia.getFin() : fin;
            if (!inicioAusencia.isBefore(finAusencia)) continue;
            if (cursor.isBefore(inicioAusencia)) resultado.add(bloque(horario, fecha, cursor, inicioAusencia, tipo, null));
            resultado.add(bloque(horario, fecha, inicioAusencia, finAusencia, "AUSENCIA", ausencia.getMotivo()));
            cursor = finAusencia;
        }
        if (cursor.isBefore(fin)) resultado.add(bloque(horario, fecha, cursor, fin, tipo, null));
    }

    private BloqueAgenda bloque(HorarioAtencion h, LocalDate fecha, LocalDateTime inicio, LocalDateTime fin, String tipo, String motivo) {
        Medico m = h.getMedico();
        Consultorio c = h.getConsultorio();
        return new BloqueAgenda(h.getIdHorario(), new MedicoAgenda(m.getIdMedico(), m.getNombres(), m.getApellidoPaterno(), m.getApellidoMaterno(), m.getCodigoMedico()),
                c == null ? null : new ConsultorioAgenda(c.getId(), c.getCodigo(), c.getNombre()), fecha, inicio.toLocalTime(), fin.toLocalTime(), h.getDuracionCita(), tipo, motivo,
                h.getEspecialidad() == null ? null : new EspecialidadAgenda(h.getEspecialidad().getIdEspecialidad(), h.getEspecialidad().getNombre()));
    }

    private static String normalizarDia(String valor) {
        String clave = valor == null ? "" : Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        for (String dia : DIAS) {
            String comparable = Normalizer.normalize(dia, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
            if (comparable.equals(clave)) return dia;
        }
        throw error(HttpStatus.BAD_REQUEST, "Selecciona un día válido de la semana");
    }

    private static String normalizarEstado(String estado) {
        String normalizado = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVO", "INACTIVO").contains(normalizado)) throw error(HttpStatus.BAD_REQUEST, "El estado debe ser ACTIVO o INACTIVO");
        return normalizado;
    }

    private static LocalDate primeraOcurrencia(LocalDate desde, String dia) {
        return desde.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(DIAS.indexOf(normalizarDia(dia)) + 1)));
    }

    private static LocalDate menorFin(LocalDate a, LocalDate b) { return a == null ? b : b == null || a.isBefore(b) ? a : b; }
    private static Long idMedico(Medico m) { return m == null ? null : m.getIdMedico(); }
    private static Long idConsultorio(Consultorio c) { return c == null ? null : c.getId(); }
    private static LocalDate hoy() { return LocalDate.now(ZONA_CLINICA); }
    private static ResponseStatusException error(HttpStatus estado, String detalle) { return new ResponseStatusException(estado, detalle); }

    private static HorarioAtencion copiar(HorarioAtencion origen) {
        HorarioAtencion copia = new HorarioAtencion();
        copiarDatos(origen, copia);
        return copia;
    }

    private static void copiarDatos(HorarioAtencion origen, HorarioAtencion destino) {
        destino.setMedico(origen.getMedico());
        destino.setConsultorio(origen.getConsultorio());
        destino.setEspecialidad(origen.getEspecialidad());
        destino.setDiaSemana(origen.getDiaSemana());
        destino.setHoraInicio(origen.getHoraInicio());
        destino.setHoraFin(origen.getHoraFin());
        destino.setDuracionCita(origen.getDuracionCita());
        destino.setEstado(origen.getEstado());
        destino.setVigenteDesde(origen.getVigenteDesde());
        destino.setVigenteHasta(origen.getVigenteHasta());
    }
}
