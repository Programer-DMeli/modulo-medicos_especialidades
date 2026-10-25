package com.tecsup.medicos_especialidades04.Controller;

import com.tecsup.medicos_especialidades04.Model.Medico;
import com.tecsup.medicos_especialidades04.Model.EstadoMedico;
import com.tecsup.medicos_especialidades04.Service.MedicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicos")
public class MedicoController {
    public record AltaMedico(@com.fasterxml.jackson.annotation.JsonUnwrapped Medico medico,
            com.tecsup.medicos_especialidades04.Model.CredencialesMedico credencialesAcceso) {}
    @Autowired
    private MedicoService service;

    @GetMapping
    public List<Medico> listar() {
        return service.listar();
    }

    @PostMapping
    public ResponseEntity<AltaMedico> guardar(@RequestBody Medico medico) {
        medico.setIdMedico(null);
        Medico saved = service.guardar(medico);
        return ResponseEntity
                .status(201)
                .body(new AltaMedico(saved, saved.getCredencialesAcceso()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medico> obtener(@PathVariable Long id) {

        Medico medico = service.obtener(id);

        if (medico == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(medico);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Medico> actualizar(
            @PathVariable Long id,
            @RequestBody Medico medico) {

        Medico existente = service.obtener(id);

        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        existente.setTipoDocumento(medico.getTipoDocumento());
        existente.setNumeroDocumento(medico.getNumeroDocumento());
        existente.setNombres(medico.getNombres());
        existente.setApellidoPaterno(medico.getApellidoPaterno());
        existente.setApellidoMaterno(medico.getApellidoMaterno());
        existente.setCmp(medico.getCmp());
        existente.setRne(medico.getRne());
        existente.setTelefono(medico.getTelefono());
        existente.setCorreo(medico.getCorreo());
        existente.setFechaIngreso(medico.getFechaIngreso());
        existente.setEstado(medico.getEstado());
        existente.setEspecialidadIds(medico.getEspecialidadIds());

        return ResponseEntity.ok(service.guardar(existente));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Medico> cambiarEstado(@PathVariable Long id, @RequestParam EstadoMedico estado) {
        return ResponseEntity.ok(service.cambiarEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        Medico medico = service.obtener(id);

        if (medico == null) {
            return ResponseEntity.notFound().build();
        }

        service.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
