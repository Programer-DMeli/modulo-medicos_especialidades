package com.tecsup.medicos_especialidades04;

import com.tecsup.medicos_especialidades04.Model.*;
import com.tecsup.medicos_especialidades04.Repository.*;
import com.tecsup.medicos_especialidades04.Service.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "entrega.admin", roles = "ADMINISTRADOR")
class ModulosEntregaTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired RolService rolesService;
    @Autowired RolRepository roles;
    @Autowired UsuarioRepository cuentas;
    @Autowired ConsultorioService consultorios;
    @Autowired EspecialidadesService especialidades;
    @Autowired JdbcTemplate jdbc;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired jakarta.persistence.EntityManager em;

    private Usuario cuenta() {
        Usuario user = new Usuario();
        user.setUsername("entrega." + UUID.randomUUID().toString().substring(0, 8));
        user.setNombres("Cuenta de verificación");
        user.setPassword("Entrega2026!");
        user.setRol(roles.findByNombre("MEDICO").orElseThrow());
        return user;
    }

    @Test
    void usuarioCrudCifraPasswordYNoLaExpone() throws Exception {
        Usuario saved = usuarios.registrar(cuenta());
        Long id = saved.getIdUsuario();
        assertTrue(encoder.matches("Entrega2026!", cuentas.findById(id).orElseThrow().getPassword()));
        mvc.perform(get("/api/usuarios/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(put("/api/usuarios/" + id).with(csrf()).contentType("application/json")
                .content("{\"username\":\"" + saved.getUsername() + "\",\"nombres\":\"Nombre actualizado\","
                        + "\"password\":\"\",\"rol\":{\"idRol\":" + saved.getRol().getIdRol() + "}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombres").value("Nombre actualizado"));
        assertTrue(encoder.matches("Entrega2026!", usuarios.obtener(id).getPassword()));
        mvc.perform(put("/api/usuarios/" + id + "/estado").with(csrf()).param("activo", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(false));
        mvc.perform(put("/api/usuarios/" + id + "/estado").with(csrf()).param("activo", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(true));
        mvc.perform(delete("/api/usuarios/" + id).with(csrf())).andExpect(status().isNoContent());
        assertFalse(usuarios.obtener(id).getEstado());
        evidencia("Usuario", id, List.of("INSERT", "UPDATE", "UPDATE", "UPDATE", "UPDATE"));
    }

    @Test
    void validacionesRechazanDatosAntesDeLlegarALaBase() {
        Usuario largo = cuenta();
        largo.setUsername("x".repeat(51));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> usuarios.registrar(largo)).getStatusCode().value());
        Usuario password = cuenta();
        password.setPassword("á".repeat(37));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> usuarios.registrar(password)).getStatusCode().value());
        Usuario correo = cuenta();
        correo.setCorreo("no-es-correo");
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> usuarios.registrar(correo)).getStatusCode().value());
        Usuario nombre = cuenta();
        nombre.setNombres("x".repeat(101));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> usuarios.registrar(nombre)).getStatusCode().value());
        Rol rol = new Rol();
        rol.setNombre("x".repeat(51));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> rolesService.registrar(rol)).getStatusCode().value());
        rol.setNombre("VERIFICACION");
        rol.setDescripcion("x".repeat(201));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> rolesService.registrar(rol)).getStatusCode().value());
    }

    @Test
    void consultorioCrudConRelacionYAuditoriaEnLaBase() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Especialidades specialty = especialidades.guardar(new Especialidades(null, "Entrega " + suffix, "", 30, true));
        Consultorio saved = consultorios.registrar(new Consultorio(null, "Sala de prueba", 1, "Consultas",
                EstadoConsultorio.DISPONIBLE, specialty));
        Long id = saved.getId();
        assertEquals(specialty.getIdEspecialidad(), jdbc.queryForObject(
                "SELECT especialidad_id FROM consultorios WHERE id = ?", Long.class, id));
        saved.setNombre("Sala actualizada");
        consultorios.actualizar(id, saved);
        assertEquals("Sala actualizada", consultorios.obtener(id).getNombre());
        consultorios.eliminar(id);
        em.flush();
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM consultorios WHERE id = ?", Integer.class, id));
        evidencia("Consultorio", id, List.of("INSERT", "UPDATE", "DELETE"));
    }

    @Test
    void menuYPlantillasRespetanRolYRenderizan() throws Exception {
        String medico = mvc.perform(get("/").with(user("doctor").roles("MEDICO")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(medico.contains("href=\"/usuarios\""));
        assertFalse(medico.contains("href=\"/roles\""));
        assertFalse(medico.contains("href=\"/consultorios\""));
        assertTrue(medico.contains("href=\"/horarios\""));
        mvc.perform(get("/consultorios").with(user("doctor").roles("MEDICO")))
                .andExpect(redirectedUrl("/403"));
        for (String page : List.of("/usuarios", "/usuarios/registrar", "/roles", "/roles/registrar", "/auditoria")) {
            mvc.perform(get(page)).andExpect(status().isOk());
            mvc.perform(get(page).with(user("recepcion").roles("RECEPCIONISTA")))
                    .andExpect(redirectedUrl("/403"));
        }
    }

    private void evidencia(String entidad, Long id, List<String> operaciones) {
        em.flush();
        var entries = jdbc.queryForList("SELECT usuario, fecha_hora, operacion, entidad, registro_id FROM auditoria "
                + "WHERE entidad = ? AND registro_id = ? ORDER BY id_auditoria", entidad, id);
        assertEquals(operaciones, entries.stream().map(row -> row.get("operacion")).toList());
        entries.forEach(row -> {
            assertEquals("entrega.admin", row.get("usuario"));
            assertNotNull(row.get("fecha_hora"));
            System.out.println("EVIDENCIA_BD " + row);
        });
    }

    @Test
    void medicoNoPuedeAdministrarPersonalNiAbrirElFormulario() throws Exception {
        for (String role : List.of("MEDICO", "RECEPCIONISTA")) {
            mvc.perform(post("/api/medicos").with(user("lector").roles(role)).with(csrf())
                    .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/api/medicos/1").with(user("lector").roles(role)).with(csrf())
                    .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/medicos/1").with(user("lector").roles(role)).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/medicos/registrar").param("id", "1").with(user("lector").roles(role)))
                    .andExpect(redirectedUrl("/403"));
        }
        String page = mvc.perform(get("/medicos").with(user("doctor").roles("MEDICO")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(page.contains("href=\"/medicos/registrar\""));
        String adminPage = mvc.perform(get("/medicos"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(adminPage.contains("href=\"/medicos/registrar\""));
    }
}
