package com.tecsup.medicos_especialidades04.Controller;

import com.tecsup.medicos_especialidades04.Model.Especialidades;
import com.tecsup.medicos_especialidades04.Service.EspecialidadesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadesController {

    @Autowired
    private EspecialidadesService service;

    // Listar especialidades
    @GetMapping
    public List<Especialidades> listar() {
        return service.listar();
    }

    // Obtener especialidad por ID
    @GetMapping("/{id}")
    public Especialidades obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    // Registrar especialidad
    @PostMapping
    public Especialidades guardar(@RequestBody Especialidades especialidad) {
        return service.guardar(especialidad);
    }

    //Activar o desactivar especialidad
    @PutMapping("/{id}/estado")
    public Especialidades cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean estado) {

        return service.cambiarEstado(id, estado);
    }

    // Eliminar especialidad
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}