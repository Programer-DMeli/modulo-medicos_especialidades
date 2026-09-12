package com.tecsup.medicos_especialidades04.Controller;

import com.tecsup.medicos_especialidades04.Model.HorarioAtencion;
import com.tecsup.medicos_especialidades04.Service.HorarioAtencionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horarios")
@CrossOrigin(origins = "*")
public class HorarioAtencionController {

    @Autowired
    private HorarioAtencionService service;

    // LISTAR TODOS LOS HORARIOS
    @GetMapping
    public List<HorarioAtencion> listar() {
        return service.listar();
    }
    // OBTENER HORARIO POR ID
    // GET: /api/horarios/{id}
    @GetMapping("/{id}")
    public ResponseEntity<HorarioAtencion> obtener(@PathVariable Long id) {

        HorarioAtencion horario = service.obtener(id);

        if (horario == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(horario);
    }
    // REGISTRAR HORARIO
    @PostMapping
    public ResponseEntity<HorarioAtencion> guardar(
            @RequestBody HorarioAtencion horario) {

        HorarioAtencion nuevoHorario = service.guardar(horario);

        return ResponseEntity.status(201).body(nuevoHorario);
    }
    // ACTUALIZAR HORARIO
    @PutMapping("/{id}")
    public ResponseEntity<HorarioAtencion> actualizar(
            @PathVariable Long id,
            @RequestBody HorarioAtencion horario) {
        HorarioAtencion actualizado = service.actualizar(id, horario);
        if (actualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(actualizado);
    }
    //eliminar Horario
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        HorarioAtencion horario = service.obtener(id);

        if (horario == null) {
            return ResponseEntity.notFound().build();
        }

        service.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
