package com.tecsup.medicos_especialidades04.Controller;

import com.tecsup.medicos_especialidades04.Model.Auditoria;
import com.tecsup.medicos_especialidades04.Service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Registro automatico en AuditoriaService, invocado desde cada servicio (INSERT/UPDATE/DELETE)
@RestController
@RequestMapping("/api/auditoria")
@CrossOrigin(origins = "*")
public class AuditoriaController {

    @Autowired
    private AuditoriaService service;

    @GetMapping
    public List<Auditoria> listar() {
        return service.listar();
    }
}
