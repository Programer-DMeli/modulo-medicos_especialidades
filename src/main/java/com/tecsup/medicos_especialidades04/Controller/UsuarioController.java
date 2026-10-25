package com.tecsup.medicos_especialidades04.Controller;

import com.tecsup.medicos_especialidades04.Model.Usuario;
import com.tecsup.medicos_especialidades04.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")

public class UsuarioController {

    @Autowired
    private UsuarioService service;
    @Autowired private com.tecsup.medicos_especialidades04.Service.CuentaMedicoService cuentasMedicos;

    public record NombreUsuario(String username) {}

    @PutMapping("/medico/{medicoId}/username")
    public NombreUsuario cambiarUsername(@PathVariable Long medicoId, @RequestBody NombreUsuario solicitud) {
        return new NombreUsuario(cuentasMedicos.cambiarUsername(medicoId, solicitud.username()));
    }

    @PostMapping("/{id}/credenciales")
    public com.tecsup.medicos_especialidades04.Model.CredencialesMedico credenciales(@PathVariable Long id) {
        return cuentasMedicos.restablecer(id);
    }

    @GetMapping
    public List<Usuario> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtener(@PathVariable Long id) {
        Usuario u = service.obtener(id);
        return u == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(u);
    }

    @PostMapping
    public ResponseEntity<Usuario> registrar(@RequestBody Usuario usuario) {
        return ResponseEntity.status(201).body(service.registrar(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Long id, @RequestBody Usuario usuario) {
        return ResponseEntity.ok(service.actualizar(id, usuario));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Usuario> cambiarEstado(@PathVariable Long id, @RequestParam Boolean activo) {
        return ResponseEntity.ok(service.cambiarEstado(id, activo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
