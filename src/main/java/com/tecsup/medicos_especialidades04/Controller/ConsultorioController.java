package com.tecsup.medicos_especialidades04.Controller;

import com.tecsup.medicos_especialidades04.Model.Consultorio;
import com.tecsup.medicos_especialidades04.Model.HorarioAtencion;
import com.tecsup.medicos_especialidades04.Service.ConsultorioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultorios")
@CrossOrigin(origins = "*")
public class ConsultorioController {

    @Autowired
    private ConsultorioService service;

    @GetMapping
    public List<Consultorio> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Consultorio> obtener(@PathVariable Long id) {
        Consultorio consultorio = service.obtener(id);

        if (consultorio == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(consultorio);
    }

    @PostMapping
    public ResponseEntity<Consultorio> registrar(@RequestBody Consultorio consultorio) {
        return ResponseEntity.status(201).body(service.registrar(consultorio));
    }

    @PutMapping("/{consultorioId}/horarios/{horarioId}")
    public ResponseEntity<HorarioAtencion> asignarConsultorio(
            @PathVariable Long consultorioId,
            @PathVariable Long horarioId) {

        return ResponseEntity.ok(
                service.asignarConsultorio(consultorioId, horarioId)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
