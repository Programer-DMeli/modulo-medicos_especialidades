package com.tecsup.medicos_especialidades04;

import com.tecsup.medicos_especialidades04.Model.*;
import com.tecsup.medicos_especialidades04.Repository.*;
import com.tecsup.medicos_especialidades04.Service.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CuentasMedicosTests {
    @Autowired MedicoService medicos;
    @Autowired MedicoRepository medicosRepo;
    @Autowired UsuarioRepository usuarios;
    @Autowired UsuarioService usuariosService;
    @Autowired CuentaMedicoService cuentas;
    @Autowired EspecialidadesService especialidades;
    @Autowired RolRepository roles;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired MockMvc mvc;
    @Autowired tools.jackson.databind.ObjectMapper json;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    void nombreDeUsuarioSeReflejaEnMedicosSinDividirApellidos() throws Exception {
        Medico medico = medicos.guardar(nuevo());
        Long id = medico.getIdMedico();
        Usuario cuenta = usuarios.findByMedico_IdMedico(id).orElseThrow();
        String payload = json.writeValueAsString(Map.of(
                "nombres", "María José de la Cruz", "correo", "maria@example.com",
                "rol", Map.of("idRol", cuenta.getRol().getIdRol())));
        mvc.perform(put("/api/usuarios/" + cuenta.getIdUsuario()).with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf()).contentType("application/json").content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombres").value("María José de la Cruz"));
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/medicos/" + id).with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombreCompleto").value("María José de la Cruz"))
                .andExpect(jsonPath("$.apellidoPaterno").value("Médico"))
                .andExpect(jsonPath("$.usuario").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/medicos").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idMedico == " + id + ")].nombreCompleto")
                        .value(org.hamcrest.Matchers.contains("María José de la Cruz")));
    }

    @Test
    void diferenciasYaGuardadasUsanNombreDeCuentaInclusoInactiva() throws Exception {
        Medico medico = medicos.guardar(nuevo());
        Long id = medico.getIdMedico();
        Usuario cuenta = usuarios.findByMedico_IdMedico(id).orElseThrow();
        cuenta.setNombres("Maria Jobs");
        cuenta.setEstado(false);
        usuarios.saveAndFlush(cuenta);
        entityManager.clear();
        mvc.perform(get("/api/medicos/" + id).with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombreCompleto").value("Maria Jobs"));
    }

    @Test
    void medicoSinCuentaConservaSuNombreCompleto() throws Exception {
        Medico medico = nuevo();
        medico.setCodigoMedico("LEG-" + medico.getNumeroDocumento());
        Long id = medicosRepo.saveAndFlush(medico).getIdMedico();
        entityManager.clear();
        mvc.perform(get("/api/medicos/" + id).with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombreCompleto").value("Prueba Médico Cuenta"));
        mvc.perform(get("/usuarios").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("<th scope=\"col\">Médico</th>"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("colspan=\"6\"")));
    }

    private Medico nuevo() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Medico m = new Medico();
        m.setNombres("Prueba"); m.setApellidoPaterno("Médico"); m.setApellidoMaterno("Cuenta");
        m.setTipoDocumento("PASAPORTE"); m.setNumeroDocumento(suffix.toUpperCase(Locale.ROOT));
        m.setCmp("CMP-" + suffix); m.setTelefono("999000111"); m.setFechaIngreso(LocalDate.now());
        m.setEstado(EstadoMedico.ACTIVO);
        var esp = especialidades.guardar(new Especialidades(null, "Cuenta " + suffix, "", 30, true));
        m.setEspecialidadIds(List.of(esp.getIdEspecialidad()));
        return m;
    }

    @Test
    void cambiarUsuarioConservaPasswordGeneradaYPermiteLogin() throws Exception {
        Medico m = medicos.guardar(nuevo());
        Usuario cuenta = usuarios.findByMedico_IdMedico(m.getIdMedico()).orElseThrow();
        String hash = cuenta.getPassword(), password = m.getCredencialesAcceso().password();
        String nuevoUsername = "doctor." + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(put("/api/usuarios/medico/" + m.getIdMedico() + "/username")
                .with(user("admin").roles("ADMINISTRADOR")).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("username", " " + nuevoUsername + " "))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(nuevoUsername))
                .andExpect(jsonPath("$.password").doesNotExist());
        entityManager.flush(); entityManager.clear();
        Usuario actual = usuarios.findByMedico_IdMedico(m.getIdMedico()).orElseThrow();
        assertEquals(hash, actual.getPassword());
        assertEquals(cuenta.getIdUsuario(), actual.getIdUsuario());
        assertEquals(cuenta.getNombres(), actual.getNombres());
        mvc.perform(formLogin().user(nuevoUsername).password(password))
                .andExpect(authenticated().withRoles("MEDICO"));
    }

    @Test
    void cambiarUsuarioValidaDuplicadosLongitudPermisosYCsrf() throws Exception {
        Medico m = medicos.guardar(nuevo());
        Usuario cuenta = usuarios.findByMedico_IdMedico(m.getIdMedico()).orElseThrow();
        Medico otro = medicos.guardar(nuevo());
        String ocupado = usuarios.findByMedico_IdMedico(otro.getIdMedico()).orElseThrow().getUsername();
        String ruta = "/api/usuarios/medico/" + m.getIdMedico() + "/username";
        for (String invalido : List.of(" ", "a".repeat(51))) {
            mvc.perform(put(ruta).with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                    .contentType("application/json").content(json.writeValueAsString(Map.of("username", invalido))))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put(ruta).with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                .contentType("application/json").content(json.writeValueAsString(Map.of("username", ocupado))))
                .andExpect(status().isConflict());
        mvc.perform(put(ruta).with(user("admin").roles("ADMINISTRADOR"))
                .contentType("application/json").content("{\"username\":\"nuevo\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(ruta).with(user(cuenta.getUsername()).roles("MEDICO")).with(csrf())
                .contentType("application/json").content("{\"username\":\"nuevo\"}"))
                .andExpect(status().isForbidden());
        assertEquals(cuenta.getUsername(), usuarios.findById(cuenta.getIdUsuario()).orElseThrow().getUsername());
    }

    @Test
    void nuevoMedicoTieneCuentaCifradaYLoginDeMedico() throws Exception {
        Medico m = medicos.guardar(nuevo());
        Usuario u = usuarios.findByMedico_IdMedico(m.getIdMedico()).orElseThrow();
        assertEquals("MEDICO", u.getRol().getNombre()); assertTrue(u.getEstado());
        assertTrue(encoder.matches(m.getCredencialesAcceso().password(), u.getPassword()));
        mvc.perform(formLogin().user(u.getUsername()).password(m.getCredencialesAcceso().password()))
                .andExpect(authenticated().withRoles("MEDICO"));
        mvc.perform(get("/api/medicos/" + m.getIdMedico()).with(user(u.getUsername()).roles("MEDICO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.credencialesAcceso").doesNotExist());
        mvc.perform(get("/api/usuarios").with(user(u.getUsername()).roles("MEDICO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sincronizacionLegadaEsIdempotenteYNoReactivaLaCuenta() {
        Medico legacy = nuevo();
        legacy.setCodigoMedico("LEG-" + legacy.getNumeroDocumento());
        Medico m = medicosRepo.saveAndFlush(legacy);
        assertNotNull(cuentas.asegurarCuenta(m));
        Usuario u = usuarios.findByMedico_IdMedico(m.getIdMedico()).orElseThrow();
        String hash = u.getPassword(); u.setEstado(false); usuarios.saveAndFlush(u);
        assertNull(cuentas.asegurarCuenta(m));
        assertEquals(hash, usuarios.findByMedico_IdMedico(m.getIdMedico()).orElseThrow().getPassword());
        assertFalse(usuarios.findByMedico_IdMedico(m.getIdMedico()).orElseThrow().getEstado());
    }

    @Test
    void altaHttpEntregaCredencialesSoloEnElAltaYRestablecerEsSoloAdmin() throws Exception {
        Medico m = nuevo();
        String payload = "{\"nombres\":\"Prueba\",\"apellidoPaterno\":\"Cuenta\",\"apellidoMaterno\":\"JPA\","
                + "\"tipoDocumento\":\"PASAPORTE\",\"numeroDocumento\":\"" + m.getNumeroDocumento() + "\","
                + "\"cmp\":\"" + m.getCmp() + "\",\"telefono\":\"999000111\",\"fechaIngreso\":\"" + LocalDate.now()
                + "\",\"estado\":\"ACTIVO\",\"especialidadIds\":[" + m.getEspecialidadIds().getFirst() + "]}";
        String body = mvc.perform(post("/api/medicos").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                .contentType("application/json").content(payload)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.idMedico").isNumber()).andExpect(jsonPath("$.credencialesAcceso.password").exists())
                .andReturn().getResponse().getContentAsString();
        Long id = json.readTree(body).get("idMedico").asLong();
        Usuario u = usuarios.findByMedico_IdMedico(id).orElseThrow();
        mvc.perform(get("/api/usuarios").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].password").doesNotExist());
        mvc.perform(post("/api/usuarios/" + u.getIdUsuario() + "/credenciales")
                .with(user("medico").roles("MEDICO")).with(csrf())).andExpect(status().isForbidden());
        String reset = mvc.perform(post("/api/usuarios/" + u.getIdUsuario() + "/credenciales")
                .with(user("admin").roles("ADMINISTRADOR")).with(csrf())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String password = json.readTree(reset).get("password").asText();
        assertTrue(encoder.matches(password, usuarios.findById(u.getIdUsuario()).orElseThrow().getPassword()));
    }

    @Test
    void borrarMedicoDesactivaSuCuentaYConservaUsuario() {
        Medico m = medicos.guardar(nuevo());
        Long uid = usuarios.findByMedico_IdMedico(m.getIdMedico()).orElseThrow().getIdUsuario();
        medicos.eliminar(m.getIdMedico());
        Usuario u = usuarios.findById(uid).orElseThrow();
        assertFalse(u.getEstado()); assertNull(u.getMedico());
    }
}
