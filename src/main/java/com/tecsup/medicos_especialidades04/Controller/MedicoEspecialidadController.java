package com.tecsup.medicos_especialidades04.Controller;

import com.tecsup.medicos_especialidades04.Model.MedicoEspecialidad;
import com.tecsup.medicos_especialidades04.Service.MedicoEspecialidadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api//medico/especialidades")
public class MedicoEspecialidadController {
    @Autowired
    private MedicoEspecialidadService service;

    @GetMapping
    public List<MedicoEspecialidad> listar() {
        return service.listar();
    }

    @PostMapping
    public ResponseEntity<MedicoEspecialidad> guardar(
            @RequestBody MedicoEspecialidad medicoEspecialidad) {

        return ResponseEntity
                .status(201)
                .body(service.guardar(medicoEspecialidad));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicoEspecialidad> obtener(
            @PathVariable Long id) {

        MedicoEspecialidad relacion = service.obtener(id);

        if (relacion == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(relacion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicoEspecialidad> actualizar(
            @PathVariable Long id,
            @RequestBody MedicoEspecialidad medicoEspecialidad) {

        MedicoEspecialidad existente = service.obtener(id);

        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        existente.setMedico(medicoEspecialidad.getMedico());
        existente.setEspecialidad(medicoEspecialidad.getEspecialidad());
        existente.setSubespecialidad(
                medicoEspecialidad.getSubespecialidad()
        );

        return ResponseEntity.ok(service.guardar(existente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        MedicoEspecialidad relacion = service.obtener(id);

        if (relacion == null) {
            return ResponseEntity.notFound().build();
        }

        service.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
