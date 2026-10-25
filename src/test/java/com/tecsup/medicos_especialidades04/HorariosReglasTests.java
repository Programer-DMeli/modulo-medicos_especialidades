package com.tecsup.medicos_especialidades04;

import com.tecsup.medicos_especialidades04.Model.*;
import com.tecsup.medicos_especialidades04.Repository.*;
import com.tecsup.medicos_especialidades04.Service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({AlcanceMedicoService.class, HorarioAtencionService.class, ConsultorioService.class, AuditoriaService.class})
class HorariosReglasTests {
    @Autowired private TestEntityManager em;
    @Autowired private HorarioAtencionService service;
    @Autowired private ConsultorioService consultorioService;
    @Autowired private HorarioAtencionRepository horarios;
    @Autowired private AusenciaMedicoRepository ausencias;
    @Autowired private MedicoRepository medicos;
    @Autowired private ConsultorioRepository consultorios;
    @Autowired private AuditoriaRepository auditorias;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoSpyBean private AuditoriaService auditoriaService;

    private LocalDate hoy() { return LocalDate.now(ZoneId.of("America/Lima")); }
    private LocalDate lunes() { return hoy().plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)); }

    private Medico medico() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Medico m = new Medico();
        m.setCodigoMedico("M-" + uid);
        m.setTipoDocumento("DNI");
        m.setNumeroDocumento(uid);
        m.setNombres("Médico prueba");
        m.setApellidoPaterno("Apellido");
        m.setApellidoMaterno("Prueba");
        m.setCmp(uid);
        m.setFechaIngreso(hoy());
        m.setEstado(EstadoMedico.ACTIVO);
        em.persistAndFlush(m);
        Especialidades especialidad = em.persistAndFlush(new Especialidades("ESP-" + uid, "Especialidad " + uid, "", 30, true));
        MedicoEspecialidad asignacion = em.persistAndFlush(new MedicoEspecialidad(m, especialidad, null));
        m.getMedicoEspecialidades().add(asignacion);
        return m;
    }

    private Consultorio consultorio() {
        return em.persistAndFlush(new Consultorio("C-" + UUID.randomUUID().toString().substring(0, 8),
                "Sala prueba", 1, "Área prueba", EstadoConsultorio.DISPONIBLE, null));
    }

    private HorarioAtencion horario(Medico m, Consultorio c, int inicio, int fin) {
        HorarioAtencion h = new HorarioAtencion();
        h.setMedico(m);
        if (!m.getMedicoEspecialidades().isEmpty()) h.setEspecialidad(m.getMedicoEspecialidades().getFirst().getEspecialidad());
        h.setConsultorio(c);
        h.setDiaSemana("LUNES");
        h.setHoraInicio(LocalTime.of(inicio, 0));
        h.setHoraFin(LocalTime.of(fin, 0));
        h.setDuracionCita(30);
        h.setEstado("ACTIVO");
        h.setVigenteDesde(lunes());
        return h;
    }

    private AusenciaMedico ausencia(Medico m, LocalDateTime inicio, LocalDateTime fin) {
        AusenciaMedico a = new AusenciaMedico();
        a.setMedico(m);
        a.setInicio(inicio);
        a.setFin(fin);
        a.setMotivo("Permiso programado");
        a.setEstado("ACTIVO");
        return a;
    }

    private void estado(int codigo, Runnable operacion) {
        assertEquals(codigo, assertThrows(ResponseStatusException.class, operacion::run).getStatusCode().value());
    }

    @Test void rechazaCruceDelMedicoAunqueCambieConsultorio() {
        Medico m = medico();
        service.guardar(horario(m, consultorio(), 8, 12));
        estado(409, () -> service.guardar(horario(m, consultorio(), 11, 14)));
    }

    @Test void rechazaCruceDelConsultorioConOtroMedico() {
        Consultorio c = consultorio();
        service.guardar(horario(medico(), c, 8, 12));
        estado(409, () -> service.guardar(horario(medico(), c, 9, 10)));
    }

    @Test void permiteBloquesContiguosYVigenciasSinOcurrenciaCompartida() {
        Medico m = medico();
        Consultorio c = consultorio();
        HorarioAtencion primero = horario(m, c, 8, 10);
        primero.setVigenteHasta(lunes().plusDays(3));
        service.guardar(primero);
        service.guardar(horario(m, c, 10, 12));
        HorarioAtencion siguiente = horario(m, c, 8, 10);
        siguiente.setVigenteDesde(lunes().plusDays(1));
        siguiente.setVigenteHasta(lunes().plusWeeks(1));
        assertNotNull(service.guardar(siguiente).getIdHorario());
    }

    @Test void permiteOtrosDiasYHorariosInactivos() {
        Medico m = medico();
        Consultorio c = consultorio();
        service.guardar(horario(m, c, 8, 12));
        HorarioAtencion martes = horario(m, c, 8, 12);
        martes.setDiaSemana("martes");
        assertEquals("Martes", service.guardar(martes).getDiaSemana());
        HorarioAtencion inactivo = horario(m, c, 8, 12);
        inactivo.setEstado("INACTIVO");
        assertNotNull(service.guardar(inactivo).getIdHorario());
    }

    @Test void actualizarExcluyePropioIdPeroNoOtrosHorarios() {
        Medico m = medico();
        Consultorio c = consultorio();
        HorarioAtencion h = service.guardar(horario(m, c, 8, 10));
        HorarioAtencion cambio = horario(m, c, 8, 10);
        cambio.setDuracionCita(20);
        assertEquals(h.getIdHorario(), service.actualizar(h.getIdHorario(), cambio).getIdHorario());
        service.guardar(horario(m, c, 10, 12));
        estado(409, () -> service.actualizar(h.getIdHorario(), horario(m, c, 9, 11)));
    }

    @Test void asignacionDeConsultorioAplicaMismasReglas() {
        Consultorio c = consultorio();
        service.guardar(horario(medico(), c, 8, 12));
        HorarioAtencion sinSala = service.guardar(horario(medico(), null, 8, 12));
        estado(409, () -> consultorioService.asignarConsultorio(c.getId(), sinSala.getIdHorario()));
        assertNull(horarios.findById(sinSala.getIdHorario()).orElseThrow().getConsultorio());
    }

    @Test void rechazaHorasInvertidasYDuracionQueNoCabe() {
        Medico m = medico();
        estado(400, () -> service.guardar(horario(m, null, 12, 8)));
        HorarioAtencion h = horario(m, null, 8, 9);
        h.setDuracionCita(61);
        estado(400, () -> service.guardar(h));
        h.setDuracionCita(0);
        estado(400, () -> service.guardar(h));
    }

    @Test void rechazaDiaEstadoVigenciaEIdInvalidos() {
        Medico m = medico();
        HorarioAtencion h = horario(m, null, 8, 12);
        h.setDiaSemana("Feriado");
        estado(400, () -> service.guardar(h));
        h.setDiaSemana("LUNES");
        h.setEstado("CUALQUIERA");
        estado(400, () -> service.guardar(h));
        h.setEstado("ACTIVO");
        h.setVigenteDesde(null);
        estado(400, () -> service.guardar(h));
        h.setVigenteDesde(lunes());
        h.setVigenteHasta(lunes().minusDays(1));
        estado(400, () -> service.guardar(h));
        h.setVigenteHasta(null);
        h.setIdHorario(123L);
        estado(400, () -> service.guardar(h));
    }

    @Test void rechazaReferenciasInexistentesYRecursosInactivos() {
        Medico inexistente = new Medico();
        inexistente.setIdMedico(Long.MAX_VALUE);
        estado(404, () -> service.guardar(horario(inexistente, null, 8, 12)));
        Medico suspendido = medico();
        suspendido.setEstado(EstadoMedico.SUSPENDIDO);
        em.flush();
        estado(400, () -> service.guardar(horario(suspendido, null, 8, 12)));
        Medico activo = medico();
        Consultorio c = consultorio();
        c.setEstado(EstadoConsultorio.INACTIVO);
        em.flush();
        estado(400, () -> service.guardar(horario(activo, c, 8, 12)));
    }

    @Test void editarBloqueYaIniciadoActualizaElMismoRegistro() {
        Medico m = medico();
        HorarioAtencion original = horario(m, consultorio(), 8, 12);
        original.setVigenteDesde(hoy());
        HorarioAtencion guardado = service.guardar(original);
        HorarioAtencion cambio = horario(m, original.getConsultorio(), 14, 18);
        cambio.setVigenteDesde(hoy().plusDays(1));
        HorarioAtencion version = service.actualizar(guardado.getIdHorario(), cambio);
        assertEquals(guardado.getIdHorario(), version.getIdHorario());
        assertEquals(cambio.getVigenteHasta(), horarios.findById(guardado.getIdHorario()).orElseThrow().getVigenteHasta());
        assertEquals(LocalTime.of(14, 0), guardado.getHoraInicio());
        assertEquals(LocalTime.of(14, 0), version.getHoraInicio());
    }

    @Test void renuevaPasadoSinAlterarHistorialYRechazaFechaAnteriorAHoy() {
        Medico m = medico();
        HorarioAtencion pasado = horario(m, null, 8, 12);
        pasado.setVigenteDesde(hoy().minusWeeks(2));
        pasado.setVigenteHasta(hoy().minusDays(1));
        em.persistAndFlush(pasado);
        HorarioAtencion renovado = service.actualizar(pasado.getIdHorario(), horario(m, null, 10, 12));
        assertNotEquals(pasado.getIdHorario(), renovado.getIdHorario());
        assertEquals(hoy().minusDays(1), pasado.getVigenteHasta());
        assertEquals(LocalTime.of(8, 0), pasado.getHoraInicio());
        HorarioAtencion retroactivo = horario(m, null, 8, 12);
        retroactivo.setVigenteDesde(hoy().minusDays(1));
        estado(400, () -> service.guardar(retroactivo));
    }

    @Test void legadoSinVigenciaNoSePublicaHastaRevision() {
        Medico m = medico();
        HorarioAtencion legado = horario(m, null, 8, 12);
        legado.setVigenteDesde(null);
        em.persistAndFlush(legado);
        assertTrue(service.agenda(lunes(), lunes(), m.getIdMedico(), null).isEmpty());
        service.actualizar(legado.getIdHorario(), horario(m, null, 8, 12));
        assertEquals("PENDIENTE", service.agenda(lunes(), lunes(), m.getIdMedico(), null).getFirst().tipo());
    }

    @Test void eliminarHistorialBorraSoloElPeriodoVencidoYAudita() {
        Medico m = medico();
        HorarioAtencion pasado = horario(m, null, 8, 12);
        pasado.setVigenteDesde(hoy().minusWeeks(2));
        pasado.setVigenteHasta(hoy().minusDays(1));
        em.persistAndFlush(pasado);
        HorarioAtencion vigente = service.guardar(horario(m, null, 8, 12));
        service.eliminar(pasado.getIdHorario());
        em.flush();
        assertFalse(horarios.existsById(pasado.getIdHorario()));
        assertTrue(horarios.existsById(vigente.getIdHorario()));
        assertTrue(auditorias.findAll().stream().anyMatch(a -> "DELETE".equals(a.getOperacion())
                && "HorarioAtencion".equals(a.getEntidad()) && pasado.getIdHorario().equals(a.getRegistroId())));
    }

    @Test void eliminarFuturoBorraYAuditaPeroIniciadoSoloCierra() {
        Medico m = medico();
        HorarioAtencion futuro = service.guardar(horario(m, null, 8, 12));
        service.eliminar(futuro.getIdHorario());
        assertFalse(horarios.existsById(futuro.getIdHorario()));
        assertTrue(auditorias.findAll().stream().anyMatch(a -> "DELETE".equals(a.getOperacion()) && futuro.getIdHorario().equals(a.getRegistroId())));
        HorarioAtencion iniciado = horario(m, null, 8, 12);
        iniciado.setVigenteDesde(hoy());
        service.guardar(iniciado);
        service.eliminar(iniciado.getIdHorario());
        assertEquals(hoy(), horarios.findById(iniciado.getIdHorario()).orElseThrow().getVigenteHasta());
    }

    @Test void agendaRecortaAusenciaParcialYConservaOtrasSemanas() {
        Medico m = medico();
        Consultorio c = consultorio();
        service.guardar(horario(m, c, 8, 12));
        service.guardarAusencia(ausencia(m, lunes().atTime(9, 0), lunes().atTime(10, 0)));
        List<HorarioAtencionService.BloqueAgenda> bloques = service.agenda(lunes(), lunes(), m.getIdMedico(), c.getId());
        assertEquals(List.of("ATENCION", "AUSENCIA", "ATENCION"), bloques.stream().map(HorarioAtencionService.BloqueAgenda::tipo).toList());
        assertEquals(List.of(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0)), bloques.stream().map(HorarioAtencionService.BloqueAgenda::horaInicio).toList());
        assertEquals("Permiso programado", bloques.get(1).motivo());
        assertEquals(1, service.agenda(lunes().plusWeeks(1), lunes().plusWeeks(1), m.getIdMedico(), null).size());
    }

    @Test void permiteRetirarHorarioAunqueMedicoYSalaDejenDeEstarActivos() {
        Medico m = medico();
        Consultorio c = consultorio();
        HorarioAtencion h = service.guardar(horario(m, c, 8, 12));
        m.setEstado(EstadoMedico.SUSPENDIDO);
        c.setEstado(EstadoConsultorio.INACTIVO);
        em.flush();
        service.eliminar(h.getIdHorario());
        assertFalse(horarios.existsById(h.getIdHorario()));
    }

    @Test void ausenciaIniciadaConservaTramoPasadoAlCancelar() {
        Medico m = medico();
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Lima"));
        // Inicio desde medianoche para evitar que un cambio de minuto afecte la prueba.
        AusenciaMedico a = service.guardarAusencia(ausencia(m, hoy().atStartOfDay(), ahora.plusDays(1)));
        LocalDateTime inicio = a.getInicio();
        service.eliminarAusencia(a.getIdAusencia());
        assertEquals("ACTIVO", a.getEstado());
        assertEquals(inicio, a.getInicio());
        assertFalse(a.getFin().isBefore(ahora));
        assertTrue(a.getFin().isBefore(ahora.plusMinutes(1)));
    }

    @Test void ausenciaDeVariosDiasSeRecortaAlHorarioYPendienteNoEsAtencion() {
        Medico m = medico();
        service.guardar(horario(m, null, 8, 12));
        service.guardarAusencia(ausencia(m, lunes().atStartOfDay(), lunes().plusDays(2).atStartOfDay()));
        var bloque = service.agenda(lunes(), lunes(), m.getIdMedico(), null).getFirst();
        assertEquals("AUSENCIA", bloque.tipo());
        assertEquals(LocalTime.of(8, 0), bloque.horaInicio());
        assertEquals(LocalTime.of(12, 0), bloque.horaFin());
        assertEquals("PENDIENTE", service.agenda(lunes().plusWeeks(1), lunes().plusWeeks(1), m.getIdMedico(), null).getFirst().tipo());
    }

    @Test void ausenciaFuturaSeAnulaSinCambiarPatronSemanal() {
        Medico m = medico();
        HorarioAtencion h = service.guardar(horario(m, null, 8, 12));
        AusenciaMedico a = service.guardarAusencia(ausencia(m, lunes().atTime(8, 0), lunes().atTime(12, 0)));
        service.eliminarAusencia(a.getIdAusencia());
        assertEquals("INACTIVO", ausencias.findById(a.getIdAusencia()).orElseThrow().getEstado());
        assertEquals("ACTIVO", horarios.findById(h.getIdHorario()).orElseThrow().getEstado());
        assertEquals("PENDIENTE", service.agenda(lunes(), lunes(), m.getIdMedico(), null).getFirst().tipo());
    }

    @Test void rechazaAusenciasInvalidasYCrucesSoloDelMismoMedico() {
        Medico m = medico();
        estado(400, () -> service.guardarAusencia(ausencia(m, lunes().atTime(12, 0), lunes().atTime(8, 0))));
        service.guardarAusencia(ausencia(m, lunes().atTime(8, 0), lunes().atTime(12, 0)));
        estado(409, () -> service.guardarAusencia(ausencia(m, lunes().atTime(9, 0), lunes().atTime(10, 0))));
        assertNotNull(service.guardarAusencia(ausencia(medico(), lunes().atTime(9, 0), lunes().atTime(10, 0))).getIdAusencia());
    }

    @Test void agendaLimitaRangoYRespetaFiltrosYVigenciaFinal() {
        Medico m = medico();
        HorarioAtencion h = horario(m, consultorio(), 8, 12);
        h.setVigenteHasta(lunes());
        service.guardar(h);
        assertEquals(1, service.agenda(lunes(), lunes().plusDays(30), m.getIdMedico(), null).size());
        estado(400, () -> service.agenda(lunes(), lunes().plusDays(31), null, null));
        estado(400, () -> service.agenda(lunes(), lunes().minusDays(1), null, null));
        assertTrue(service.agenda(lunes(), lunes(), medico().getIdMedico(), null).isEmpty());
    }

    @Test void altaRegistraInsertConElIdentificadorGenerado() {
        HorarioAtencion h = service.guardar(horario(medico(), null, 8, 12));
        assertTrue(auditorias.findAll().stream().anyMatch(a -> "HorarioAtencion".equals(a.getEntidad())
                && h.getIdHorario().equals(a.getRegistroId()) && "INSERT".equals(a.getOperacion())));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void falloDeAuditoriaRevierteHorario() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Medico m = tx.execute(status -> medico());
        long antes = horarios.count();
        try {
            doThrow(new IllegalStateException("fallo simulado")).when(auditoriaService)
                    .registrarOperacion(eq("INSERT"), eq("HorarioAtencion"), anyLong());
            assertThrows(IllegalStateException.class, () -> service.guardar(horario(m, null, 8, 12)));
            assertEquals(antes, horarios.count());
        } finally {
            reset(auditoriaService);
            tx.executeWithoutResult(status -> medicos.deleteById(m.getIdMedico()));
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void altasConcurrentesDelMismoConsultorioSoloConfirmanUna() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Medico a = tx.execute(status -> medico());
        Medico b = tx.execute(status -> medico());
        Consultorio sala = tx.execute(status -> consultorio());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (Medico m : List.of(a, b)) {
                resultados.add(executor.submit(() -> {
                    listos.countDown();
                    assertTrue(iniciar.await(10, TimeUnit.SECONDS));
                    try {
                        service.guardar(horario(m, sala, 8, 12));
                        return 201;
                    } catch (ResponseStatusException e) {
                        return e.getStatusCode().value();
                    }
                }));
            }
            assertTrue(listos.await(10, TimeUnit.SECONDS));
            iniciar.countDown();
            List<Integer> estados = new ArrayList<>();
            for (Future<Integer> resultado : resultados) estados.add(resultado.get(15, TimeUnit.SECONDS));
            Collections.sort(estados);
            assertEquals(List.of(201, 409), estados);
        } finally {
            executor.shutdownNow();
            tx.executeWithoutResult(status -> {
                horarios.findAll().stream().filter(h -> sala.getId().equals(h.getConsultorio() == null ? null : h.getConsultorio().getId())).forEach(horarios::delete);
                horarios.flush();
                medicos.deleteById(a.getIdMedico());
                medicos.deleteById(b.getIdMedico());
                consultorios.deleteById(sala.getId());
            });
        }
    }
}
