package com.tecsup.medicos_especialidades04;

import com.tecsup.medicos_especialidades04.Model.*;
import com.tecsup.medicos_especialidades04.Repository.*;
import com.tecsup.medicos_especialidades04.Service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PanelMedicoTests {
    @Autowired MockMvc mvc;
    @Autowired MedicoService medicos;
    @Autowired EspecialidadesService especialidades;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired HorarioAtencionRepository horarios;
    @Autowired ConsultorioRepository consultorios;
    @Autowired AusenciaMedicoRepository ausencias;
    @Autowired MedicoEspecialidadRepository asignaciones;
    Medico a, b;
    Usuario ua, ub;
    Especialidades comun, exclusiva;
    Consultorio compartido, ajeno;
    HorarioAtencion ha, hb;
    LocalDate lunes = LocalDate.of(2030, 1, 7);

    @BeforeEach
    void datos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        comun = especialidades.guardar(new Especialidades(null, "Común " + sufijo, "", 30, true));
        exclusiva = especialidades.guardar(new Especialidades(null, "Exclusiva " + sufijo, "", 30, true));
        a = medico("Propio", sufijo + "A", List.of(comun.getIdEspecialidad()));
        b = medico("Ajeno", sufijo + "B", List.of(comun.getIdEspecialidad(), exclusiva.getIdEspecialidad()));
        ua = usuarios.findByMedico_IdMedico(a.getIdMedico()).orElseThrow();
        ub = usuarios.findByMedico_IdMedico(b.getIdMedico()).orElseThrow();
        compartido = consultorios.save(new Consultorio("C" + sufijo, "Compartido", 1, "A", EstadoConsultorio.DISPONIBLE, comun));
        ajeno = consultorios.save(new Consultorio("D" + sufijo, "Ajeno", 2, "B", EstadoConsultorio.DISPONIBLE, exclusiva));
        ha = horario(a, compartido, 8);
        hb = horario(b, compartido, 10);
        horario(b, ajeno, 12);
        for (Medico m : List.of(a, b)) {
            AusenciaMedico ausencia = new AusenciaMedico();
            ausencia.setMedico(m); ausencia.setInicio(lunes.atTime(8, 30)); ausencia.setFin(lunes.atTime(9, 0));
            ausencia.setMotivo("Ausencia " + m.getNombres()); ausencia.setEstado("ACTIVO");
            ausencias.saveAndFlush(ausencia);
        }
    }

    Medico medico(String nombre, String documento, List<Long> ids) {
        Medico m = new Medico();
        m.setNombres(nombre); m.setApellidoPaterno("Panel"); m.setApellidoMaterno("Test");
        m.setTipoDocumento("PASAPORTE"); m.setNumeroDocumento(documento.toUpperCase(Locale.ROOT));
        m.setCmp(documento); m.setTelefono("999000111"); m.setFechaIngreso(LocalDate.of(2020, 1, 1));
        m.setEspecialidadIds(ids); m.setEstado(EstadoMedico.ACTIVO);
        return medicos.guardar(m);
    }

    HorarioAtencion horario(Medico m, Consultorio c, int hora) {
        HorarioAtencion h = new HorarioAtencion();
        h.setMedico(m); h.setConsultorio(c); h.setEspecialidad(c.getEspecialidad()); h.setDiaSemana("Lunes");
        h.setHoraInicio(LocalTime.of(hora, 0)); h.setHoraFin(LocalTime.of(hora + 1, 0));
        h.setDuracionCita(30); h.setEstado("ACTIVO"); h.setVigenteDesde(lunes);
        return horarios.saveAndFlush(h);
    }

    String leer(String url, Usuario u) throws Exception {
        return mvc.perform(get(url).with(user(u.getUsername()).roles("MEDICO"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void agendaUsaElMismoNombreQueLaCuentaVinculada() throws Exception {
        ua.setNombres("María José de la Cruz");
        usuarios.saveAndFlush(ua);
        mvc.perform(get("/api/horarios/agenda?desde=2030-01-07&hasta=2030-01-07")
                .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.medico.idMedico == " + a.getIdMedico() + ")].medico.nombreCompleto",
                        everyItem(is("María José de la Cruz"))));
    }

    @Test
    void cadaCuentaSoloRecibeSusDatosInclusoConFiltrosManipulados() throws Exception {
        for (Usuario u : List.of(ua, ub)) {
            Medico propio = u.getMedico(), otro = u == ua ? b : a;
            mvc.perform(get("/api/medicos?medicoId=" + otro.getIdMedico() + "&q=" + otro.getNombres())
                    .with(user(u.getUsername()).roles("MEDICO")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].idMedico").value(propio.getIdMedico()));
            for (String ruta : List.of("/api/medicos", "/api/horarios", "/api/horarios/ausencias", "/api/medico/especialidades",
                    "/api/horarios/agenda?desde=2030-01-07&hasta=2030-01-07")) {
                String json = leer(ruta, u);
                assertTrue(json.contains(propio.getNombres()), ruta);
                assertFalse(json.contains("\"nombres\":\"" + otro.getNombres() + "\""), ruta);
                assertFalse(json.contains("password")); assertFalse(json.contains("credencialesAcceso"));
            }
            mvc.perform(get("/api/especialidades").with(user(u.getUsername()).roles("MEDICO")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(u == ua ? 1 : 2));
            for (String ruta : List.of("/", "/medicos", "/especialidades", "/horarios", "/horarios/agenda", "/horarios/ausencias")) {
                String html = leer(ruta, u);
                assertTrue(html.contains("Mi perfil")); assertTrue(html.contains("Mis especialidades")); assertTrue(html.contains("Mi agenda"));
                assertFalse(html.contains("href=\"/horarios/registrar\""));
            }
        }
    }

    @Test
    void idsAjenosYFiltrosDeAgendaNoPermitenEscaparDelAlcance() throws Exception {
        for (Usuario u : List.of(ua, ub)) {
            Medico otro = u == ua ? b : a;
            HorarioAtencion otroHorario = u == ua ? hb : ha;
            Long relacion = asignaciones.findByMedico_IdMedico(otro.getIdMedico()).getFirst().getIdMedicoEspecialidad();
            for (String ruta : List.of("/api/medicos/" + otro.getIdMedico(), "/api/horarios/" + otroHorario.getIdHorario(),
                    "/api/medico/especialidades/" + relacion,
                    "/api/horarios/agenda?desde=2030-01-07&hasta=2030-01-07&medicoId=" + otro.getIdMedico())) {
                mvc.perform(get(ruta).with(user(u.getUsername()).roles("MEDICO"))).andExpect(status().isNotFound());
            }
            leer("/api/medicos/" + u.getMedicoId(), u);
        }
        for (String ruta : List.of("/api/especialidades/" + exclusiva.getIdEspecialidad(), "/api/consultorios/" + ajeno.getId(),
                "/api/horarios/agenda?desde=2030-01-07&hasta=2030-01-07&consultorioId=" + ajeno.getId())) {
            mvc.perform(get(ruta).with(user(ua.getUsername()).roles("MEDICO"))).andExpect(status().isNotFound());
        }
        String json = leer("/api/consultorios/" + compartido.getId(), ua);
        assertFalse(json.contains("horarios")); assertFalse(json.contains("medicoEspecialidades"));
        assertFalse(leer("/api/especialidades/" + comun.getIdEspecialidad(), ua).contains("medicoEspecialidades"));
        mvc.perform(get("/api/consultorios").with(user(ua.getUsername()).roles("MEDICO")))
                .andExpect(jsonPath("$.length()").value(1));
        assertFalse(leer("/api/horarios/agenda?desde=2030-01-07&hasta=2030-01-07&consultorioId=" + compartido.getId(), ua)
                .contains("\"nombres\":\"Ajeno\""));
    }

    @Test
    void sinVinculoBloqueaTodasLasConsultasYExplicaElProblema() throws Exception {
        Usuario sin = new Usuario(); sin.setUsername("sin-" + UUID.randomUUID().toString().substring(0, 8));
        sin.setNombres("Sin vínculo"); sin.setPassword("hash-no-utilizado"); sin.setRol(roles.findByNombre("MEDICO").orElseThrow());
        usuarios.saveAndFlush(sin);
        for (String ruta : List.of("/api/medicos", "/api/medicos/" + a.getIdMedico(), "/api/especialidades", "/api/especialidades/" + comun.getIdEspecialidad(),
                "/api/medico/especialidades", "/api/horarios", "/api/horarios/" + ha.getIdHorario(), "/api/horarios/ausencias",
                "/api/consultorios", "/api/consultorios/" + compartido.getId(), "/api/horarios/agenda?desde=2030-01-07&hasta=2030-01-07")) {
            mvc.perform(get(ruta).with(user(sin.getUsername()).roles("MEDICO"))).andExpect(status().isForbidden());
        }
        for (String ruta : List.of("/", "/medicos", "/especialidades", "/horarios", "/horarios/agenda", "/horarios/ausencias")) {
            mvc.perform(get(ruta).with(user(sin.getUsername()).roles("MEDICO"))).andExpect(view().name("medico-sin-vinculo"))
                    .andExpect(content().string(containsString("Las consultas personales están bloqueadas")));
        }
    }

    @Test
    void medicoNoPuedeEscribirNiAbrirFormularios() throws Exception {
        for (String ruta : List.of("/api/medicos", "/api/especialidades", "/api/medico/especialidades", "/api/consultorios", "/api/horarios", "/api/horarios/ausencias")) {
            mvc.perform(post(ruta).with(user(ua.getUsername()).roles("MEDICO")).with(csrf()).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(put(ruta + "/1").with(user(ua.getUsername()).roles("MEDICO")).with(csrf()).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete(ruta + "/1").with(user(ua.getUsername()).roles("MEDICO")).with(csrf())).andExpect(status().isForbidden());
        }
        for (String ruta : List.of("/medicos/registrar", "/especialidades/registrar", "/especialidades/estado", "/horarios/registrar", "/horarios/editar/1", "/consultorios/asignar", "/consultorios/registrar")) {
            mvc.perform(get(ruta).with(user(ua.getUsername()).roles("MEDICO"))).andExpect(redirectedUrl("/403"));
        }
    }

    @Test
    void administracionYRecepcionConservanLecturaGeneralYProgramacion() throws Exception {
        for (String rol : List.of("ADMINISTRADOR", "RECEPCIONISTA")) {
            mvc.perform(get("/api/medicos").with(user("operador").roles(rol)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[*].idMedico", hasItems(a.getIdMedico().intValue(), b.getIdMedico().intValue())));
            for (String ruta : List.of("/api/medicos/" + b.getIdMedico(), "/api/horarios/" + hb.getIdHorario(), "/api/especialidades/" + exclusiva.getIdEspecialidad(),
                    "/api/consultorios/" + ajeno.getId(), "/horarios/registrar", "/consultorios/asignar")) {
                mvc.perform(get(ruta).with(user("operador").roles(rol))).andExpect(status().isOk());
            }
        }
        String hash = ua.getPassword(); Boolean estado = ua.getEstado();
        leer("/api/medicos", ua);
        assertEquals(hash, usuarios.findById(ua.getIdUsuario()).orElseThrow().getPassword());
        assertEquals(estado, ua.getEstado());
    }
    @Test
    void cambiosDeEstadoDesdeTablasRespetanRolesYNoModificanCredenciales() throws Exception {
        String hash = ua.getPassword();
        for (String ruta : List.of("/api/medicos/" + a.getIdMedico() + "/estado?estado=SUSPENDIDO",
                "/api/consultorios/" + compartido.getId() + "/estado?estado=INACTIVO",
                "/api/horarios/" + ha.getIdHorario() + "/estado?estado=INACTIVO")) {
            mvc.perform(put(ruta).with(user(ua.getUsername()).roles("MEDICO")).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(put("/api/medicos/" + a.getIdMedico() + "/estado?estado=SUSPENDIDO")
                .with(user("recepcion").roles("RECEPCIONISTA")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/medicos/" + a.getIdMedico() + "/estado?estado=SUSPENDIDO")
                .with(user("admin").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("SUSPENDIDO"));
        mvc.perform(put("/api/consultorios/" + ajeno.getId() + "/estado?estado=INACTIVO")
                .with(user("recepcion").roles("RECEPCIONISTA")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("INACTIVO"));
        mvc.perform(put("/api/horarios/" + ha.getIdHorario() + "/estado?estado=INACTIVO")
                .with(user("recepcion").roles("RECEPCIONISTA")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("INACTIVO"));
        assertEquals(hash, usuarios.findById(ua.getIdUsuario()).orElseThrow().getPassword());
        assertTrue(usuarios.findById(ua.getIdUsuario()).orElseThrow().getEstado());
    }

    @Test
    void cambiarEstadoDeHorarioIniciadoConservaSuHistoria() throws Exception {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        ha.setVigenteDesde(hoy.minusWeeks(2)); horarios.saveAndFlush(ha);
        mvc.perform(put("/api/horarios/" + ha.getIdHorario() + "/estado?estado=INACTIVO")
                .with(user("recepcion").roles("RECEPCIONISTA")).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("INACTIVO"))
                .andExpect(jsonPath("$.vigenteDesde").value(hoy.plusDays(1).toString()))
                .andExpect(jsonPath("$.idHorario", not(ha.getIdHorario().intValue())));
        assertEquals("ACTIVO", horarios.findById(ha.getIdHorario()).orElseThrow().getEstado());
        assertEquals(hoy, horarios.findById(ha.getIdHorario()).orElseThrow().getVigenteHasta());
        mvc.perform(put("/api/horarios/" + ha.getIdHorario() + "/estado?estado=INACTIVO")
                .with(user("admin").roles("ADMINISTRADOR")).with(csrf())).andExpect(status().isConflict());
    }

    @Test
    void activacionDesdeTablaValidaCrucesEstadosYCsrf() throws Exception {
        HorarioAtencion conflicto = horario(a, compartido, 8);
        conflicto.setEstado("INACTIVO"); horarios.saveAndFlush(conflicto);
        mvc.perform(put("/api/horarios/" + conflicto.getIdHorario() + "/estado?estado=ACTIVO")
                .with(user("recepcion").roles("RECEPCIONISTA")).with(csrf())).andExpect(status().isConflict());
        assertEquals("INACTIVO", horarios.findById(conflicto.getIdHorario()).orElseThrow().getEstado());
        for (String ruta : List.of("/api/medicos/" + a.getIdMedico() + "/estado?estado=VACACIONES",
                "/api/consultorios/" + compartido.getId() + "/estado?estado=OCUPADO",
                "/api/horarios/" + ha.getIdHorario() + "/estado?estado=INVALIDO")) {
            mvc.perform(put(ruta).with(user("admin").roles("ADMINISTRADOR")).with(csrf())).andExpect(status().isBadRequest());
            mvc.perform(put(ruta).with(user("admin").roles("ADMINISTRADOR"))).andExpect(status().isForbidden());
        }
    }

    private String edicionHorario(LocalDate desde, LocalDate hasta, int duracion) {
        return """
            {"medico":{"idMedico":%d},"especialidad":{"idEspecialidad":%d},"consultorio":{"id":%d},
             "diaSemana":"Lunes","horaInicio":"08:00","horaFin":"09:00","duracionCita":%d,
             "estado":"ACTIVO","vigenteDesde":"%s","vigenteHasta":"%s"}
            """.formatted(a.getIdMedico(), comun.getIdEspecialidad(), compartido.getId(), duracion, desde, hasta);
    }

    @Test
    void recepcionistaEditaRepetidamenteSinCrearOtrosRegistros() throws Exception {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        ha.setVigenteDesde(hoy.minusWeeks(2)); horarios.saveAndFlush(ha);
        long cantidad = horarios.count();
        Long id = ha.getIdHorario();
        LocalDate desde = hoy.minusWeeks(2), hasta = hoy.plusWeeks(4);
        mvc.perform(put("/api/horarios/" + ha.getIdHorario()).with(user("recepcion").roles("RECEPCIONISTA"))
                .with(csrf()).contentType("application/json").content(edicionHorario(desde, hasta, 45)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duracionCita").value(45))
                .andExpect(jsonPath("$.vigenteDesde").value(desde.toString()))
                .andExpect(jsonPath("$.vigenteHasta").value(hasta.toString()))
                .andExpect(jsonPath("$.idHorario").value(id));
        assertEquals(cantidad, horarios.count());
        mvc.perform(put("/api/horarios/" + id).with(user("recepcion").roles("RECEPCIONISTA"))
                .with(csrf()).contentType("application/json").content(edicionHorario(desde, hasta, 20)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.idHorario").value(id));
        assertEquals(cantidad, horarios.count());
        assertEquals(20, horarios.findById(id).orElseThrow().getDuracionCita());
        assertEquals(hasta, horarios.findById(id).orElseThrow().getVigenteHasta());
    }

    @Test
    void editarVigenciaExigeUnDiaDeAtencionYPermiteCorregirElRango() throws Exception {
        mvc.perform(put("/api/horarios/" + ha.getIdHorario()).with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf()).contentType("application/json").content(edicionHorario(lunes.plusDays(1), lunes.plusDays(2), 45)))
                .andExpect(status().isBadRequest()).andExpect(result -> assertEquals(
                    "La vigencia debe incluir al menos un día del horario semanal",
                    ((org.springframework.web.server.ResponseStatusException) result.getResolvedException()).getReason()));
        assertEquals(30, horarios.findById(ha.getIdHorario()).orElseThrow().getDuracionCita());
        mvc.perform(put("/api/horarios/" + ha.getIdHorario()).with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf()).contentType("application/json").content(edicionHorario(lunes.plusDays(1), lunes.plusWeeks(1), 45)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duracionCita").value(45))
                .andExpect(jsonPath("$.vigenteHasta").value(lunes.plusWeeks(1).toString()));
    }

    @Test
    void horarioQueTerminaHoyActualizaElMismoRegistro() throws Exception {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        ha.setVigenteDesde(hoy.minusWeeks(2)); ha.setVigenteHasta(hoy); horarios.saveAndFlush(ha);
        mvc.perform(put("/api/horarios/" + ha.getIdHorario()).with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf()).contentType("application/json").content(edicionHorario(hoy.plusDays(1), hoy.plusWeeks(4), 45)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idHorario").value(ha.getIdHorario()))
                .andExpect(jsonPath("$.vigenteDesde").value(hoy.plusDays(1).toString()));
        assertEquals(45, horarios.findById(ha.getIdHorario()).orElseThrow().getDuracionCita());
        assertEquals(hoy.plusWeeks(4), horarios.findById(ha.getIdHorario()).orElseThrow().getVigenteHasta());
    }

    @Test
    void eliminarHistorialPorHttpRespetaPermisosYConservaOtrosPeriodos() throws Exception {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        ha.setVigenteDesde(hoy.minusWeeks(3)); ha.setVigenteHasta(hoy.minusDays(1));
        horarios.saveAndFlush(ha);
        String ruta = "/api/horarios/" + ha.getIdHorario();
        mvc.perform(delete(ruta).with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
        mvc.perform(delete(ruta).with(user(ua.getUsername()).roles("MEDICO")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete(ruta).with(user("admin").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().isNoContent());
        assertFalse(horarios.existsById(ha.getIdHorario()));
        assertTrue(horarios.existsById(hb.getIdHorario()));
        mvc.perform(get(ruta).with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isNotFound());
    }

    @Test
    void renovarVencidoDesdeHoyConservaHistorialYValidaCruces() throws Exception {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        LocalDate inicioAnterior = hoy.minusWeeks(3), finAnterior = hoy.minusDays(1);
        ha.setVigenteDesde(inicioAnterior); ha.setVigenteHasta(finAnterior); horarios.saveAndFlush(ha);
        String renovacion = edicionHorario(hoy, hoy.plusWeeks(4), 45);
        mvc.perform(put("/api/horarios/" + ha.getIdHorario()).with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf()).contentType("application/json").content(renovacion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idHorario", not(ha.getIdHorario().intValue())))
                .andExpect(jsonPath("$.vigenteDesde").value(hoy.toString()))
                .andExpect(jsonPath("$.vigenteHasta").value(hoy.plusWeeks(4).toString()));
        HorarioAtencion anterior = horarios.findById(ha.getIdHorario()).orElseThrow();
        assertEquals(inicioAnterior, anterior.getVigenteDesde());
        assertEquals(finAnterior, anterior.getVigenteHasta());
        assertEquals(30, anterior.getDuracionCita());
        mvc.perform(put("/api/horarios/" + ha.getIdHorario()).with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf()).contentType("application/json").content(renovacion))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/horarios/" + ha.getIdHorario()).with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf()).contentType("application/json").content(edicionHorario(hoy.minusDays(1), hoy.plusWeeks(4), 45)))
                .andExpect(status().isBadRequest());
    }

}
