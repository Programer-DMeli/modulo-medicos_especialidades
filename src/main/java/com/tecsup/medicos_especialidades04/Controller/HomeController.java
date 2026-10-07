package com.tecsup.medicos_especialidades04.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {
    @GetMapping("/")
    public String inicio() {
        return "index";
    }

    @GetMapping("/medicos")
    public String listarMedicos() {
        return "medicos/listar";
    }

    @GetMapping("/medicos/registrar")
    public String registrarMedico() {
        return "medicos/registrar";
    }

    @GetMapping("/especialidades")
    public String listarEspecialidades() {
        return "especialidades/listar";
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
        return "horarios/listar";
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

    // TODO (Luis): implementar /usuarios/editar/{id} y /roles/editar/{id}
}
