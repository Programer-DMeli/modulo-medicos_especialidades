package com.tecsup.medicos_especialidades04;

import com.tecsup.medicos_especialidades04.Model.Rol;
import com.tecsup.medicos_especialidades04.Model.Usuario;
import com.tecsup.medicos_especialidades04.Repository.AuditoriaRepository;
import com.tecsup.medicos_especialidades04.Repository.RolRepository;
import com.tecsup.medicos_especialidades04.Service.UsuarioService;
import java.util.UUID;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import com.tecsup.medicos_especialidades04.Service.RolService;
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
class SeguridadUsuariosRolesTests {
    @Autowired MockMvc mvc;
    @Autowired RolRepository roles;
    @Autowired UsuarioService usuarios;
    @Autowired RolService rolesService;
    @Autowired AuditoriaRepository auditoria;

    @Test
    void apiSinSesionDevuelveJson401() throws Exception {
        mvc.perform(get("/api/medicos")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void consultasYAdministracionRespetanRoles() throws Exception {
        mvc.perform(get("/api/medicos").with(user("recepcion").roles("RECEPCIONISTA")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/horarios").with(user("medico").roles("MEDICO")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").with(user("recepcion").roles("RECEPCIONISTA")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").exists());
        mvc.perform(post("/api/especialidades").with(user("medico").roles("MEDICO"))
                .with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/horarios").with(user("medico").roles("MEDICO"))
                .with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfEsObligatorioParaModificar() throws Exception {
        mvc.perform(post("/api/roles").with(user("admin").roles("ADMINISTRADOR"))
                .contentType("application/json").content("{\"nombre\":\"SIN_TOKEN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "evaluador.admin", roles = "ADMINISTRADOR")
    void rolesCrudYAuditoriaConUsuarioReal() throws Exception {
        String name = ("PRUEBA_" + UUID.randomUUID().toString().substring(0, 8)).toUpperCase(Locale.ROOT);
        mvc.perform(post("/api/roles").with(csrf()).contentType("application/json")
                .content("{\"nombre\":\"" + name + "\",\"descripcion\":\"Temporal\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value(true));
        Rol role = roles.findByNombre(name).orElseThrow();
        Long id = role.getIdRol();
        mvc.perform(put("/api/roles/" + id).with(csrf()).contentType("application/json")
                .content("{\"nombre\":\"" + name + "\",\"descripcion\":\"Editado\",\"estado\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(false));
        mvc.perform(put("/api/roles/" + id).with(csrf()).contentType("application/json")
                .content("{\"nombre\":\"" + name + "\",\"descripcion\":\"Estado conservado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(false));
        mvc.perform(put("/api/roles/" + id + "/estado").with(csrf()).param("activo", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(true));
        mvc.perform(delete("/api/roles/" + id).with(csrf())).andExpect(status().isNoContent());
        var entries = auditoria.findAll().stream().filter(a -> "Rol".equals(a.getEntidad())
                && id.equals(a.getRegistroId())).toList();
        assertTrue(entries.stream().anyMatch(a -> "INSERT".equals(a.getOperacion())));
        assertTrue(entries.stream().anyMatch(a -> "UPDATE".equals(a.getOperacion())));
        assertTrue(entries.stream().anyMatch(a -> "DELETE".equals(a.getOperacion())));
        assertTrue(entries.stream().allMatch(a -> "evaluador.admin".equals(a.getUsuario()) && a.getFechaHora() != null));
    }

    @Test
    void loginYBloqueoDeUsuarioORolInactivo() throws Exception {
        Rol role = roles.findByNombre("MEDICO").orElseThrow();
        Usuario user = crearUsuario(role);
        mvc.perform(formLogin().user(user.getUsername()).password("Prueba2026!"))
                .andExpect(authenticated().withUsername(user.getUsername()));
        usuarios.cambiarEstado(user.getIdUsuario(), false);
        mvc.perform(formLogin().user(user.getUsername()).password("Prueba2026!"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error=true"));
        usuarios.cambiarEstado(user.getIdUsuario(), true);
        role.setEstado(false);
        roles.saveAndFlush(role);
        mvc.perform(formLogin().user(user.getUsername()).password("Prueba2026!"))
                .andExpect(unauthenticated());
        Usuario another = datosUsuario(role);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> usuarios.registrar(another));
    }

    private Usuario crearUsuario(Rol role) { return usuarios.registrar(datosUsuario(role)); }

    @Test
    void conservaUltimoAdministradorActivo() {
        Rol admin = roles.findByNombre("ADMINISTRADOR").orElseThrow();
        Rol medico = roles.findByNombre("MEDICO").orElseThrow();
        Usuario first = crearUsuario(admin);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> usuarios.desactivar(first.getIdUsuario()));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> rolesService.cambiarEstado(admin.getIdRol(), false));
        Usuario edit = datosUsuario(medico);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> usuarios.actualizar(first.getIdUsuario(), edit));
        Usuario second = crearUsuario(admin);
        assertFalse(usuarios.cambiarEstado(first.getIdUsuario(), false).getEstado());
        assertTrue(usuarios.obtener(second.getIdUsuario()).getEstado());
    }

    @Test
    void desactivarUsuarioORolRevocaSesionExistente() throws Exception {
        Rol role = roles.findByNombre("MEDICO").orElseThrow();
        Usuario user = crearUsuario(role);
        MockHttpSession session = (MockHttpSession) mvc.perform(formLogin()
                .user(user.getUsername()).password("Prueba2026!")).andExpect(authenticated())
                .andReturn().getRequest().getSession(false);
        usuarios.cambiarEstado(user.getIdUsuario(), false);
        mvc.perform(get("/api/medicos").session(session)).andExpect(status().isUnauthorized());

        usuarios.cambiarEstado(user.getIdUsuario(), true);
        MockHttpSession secondSession = (MockHttpSession) mvc.perform(formLogin()
                .user(user.getUsername()).password("Prueba2026!")).andExpect(authenticated())
                .andReturn().getRequest().getSession(false);
        rolesService.cambiarEstado(role.getIdRol(), false);
        mvc.perform(get("/api/medicos").session(secondSession)).andExpect(status().isUnauthorized());
    }

    private Usuario datosUsuario(Rol role) {
        Usuario user = new Usuario();
        user.setUsername("prueba." + UUID.randomUUID().toString().substring(0, 8));
        user.setNombres("Usuario ficticio de prueba");
        user.setPassword("Prueba2026!");
        user.setRol(role);
        return user;
    }
}
