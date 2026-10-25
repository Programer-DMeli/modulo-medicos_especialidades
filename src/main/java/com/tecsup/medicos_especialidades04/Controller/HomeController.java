package com.tecsup.medicos_especialidades04.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {
    @org.springframework.beans.factory.annotation.Autowired
    private com.tecsup.medicos_especialidades04.Service.AlcanceMedicoService alcance;

    private String consulta(String vista) {
        if (alcance.esMedico()) {
            try { alcance.medicoId(); }
            catch (org.springframework.web.server.ResponseStatusException ex) { return "medico-sin-vinculo"; }
        }
        return vista;
    }

    @GetMapping("/")
    public String inicio() {
        return consulta("index");
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/403")
    public String accesoDenegado() {
        return "403";
    }

    @GetMapping("/medicos")
    public String listarMedicos() {
        return consulta("medicos/listar");
    }

    @GetMapping("/medicos/registrar")
    public String registrarMedico() {
        return "medicos/registrar";
    }

    @GetMapping("/especialidades")
    public String listarEspecialidades() {
        return consulta("especialidades/listar");
    }

    @GetMapping("/especialidades/registrar")
    public String registrarEspecialidad() {
        return "especialidades/registrar";
    }

    @GetMapping("/especialidades/estado")
    public String estadoEspecialidades() {
        return "especialidades/estado";
    }

    @GetMapping("/horarios")
    public String listarHorarios() {
        return consulta("horarios/listar");
    }

    @GetMapping("/horarios/agenda")
    public String agendaHorarios() {
        return consulta("horarios/agenda");
    }

    @GetMapping("/horarios/ausencias")
    public String ausenciasHorarios() {
        return consulta("horarios/ausencias");
    }

    @GetMapping("/horarios/registrar")
    public String registrarHorario() {
        return "horarios/registrar";
    }

    @GetMapping("/horarios/editar/{id}")
    public String editarHorario(@PathVariable Long id, Model model) {
        model.addAttribute("horarioId", id);
        return "horarios/editar";
    }

    @GetMapping("/consultorios")
    public String listarConsultorios() {
        return "consultorios/listar";
    }

    @GetMapping("/consultorios/registrar")
    public String registrarConsultorio() {
        return "consultorios/registrar";
    }

    @GetMapping("/consultorios/asignar")
    public String asignarConsultorio() {
        return "consultorios/asignar";
    }

    // === MODULO USUARIOS/ROLES (Pregunta 4 - Luis) ===
    @GetMapping("/usuarios")
    public String listarUsuarios() {
        return "usuarios/listar";
    }

    @GetMapping("/usuarios/registrar")
    public String registrarUsuario() {
        return "usuarios/registrar";
    }

    @GetMapping("/roles")
    public String listarRoles() {
        return "roles/listar";
    }

    @GetMapping("/roles/registrar")
    public String registrarRol() {
        return "roles/registrar";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuario(@PathVariable Long id, Model model) {
        model.addAttribute("usuarioId", id);
        return "usuarios/editar";
    }

    @GetMapping("/roles/editar/{id}")
    public String editarRol(@PathVariable Long id, Model model) {
        model.addAttribute("rolId", id);
        return "roles/editar";
    }

    @GetMapping("/auditoria")
    public String listarAuditoria() {
        return "auditoria/listar";
    }
}
