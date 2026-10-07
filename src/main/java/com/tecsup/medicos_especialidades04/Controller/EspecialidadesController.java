package com.tecsup.medicos_especialidades04.Controller;

import com.tecsup.medicos_especialidades04.Model.Especialidades;
import com.tecsup.medicos_especialidades04.Service.EspecialidadesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    // Manejar errores de duplicado (nombre o código ya existente)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }
}