package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.*;
import com.tecsup.medicos_especialidades04.Repository.*;
import com.tecsup.medicos_especialidades04.config.SessionRevocation;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
@Transactional
public class CuentaMedicoService {
    private final UsuarioRepository usuarios;
    private final MedicoRepository medicos;
    private final RolRepository roles;
    private final BCryptPasswordEncoder passwords;
    private final AuditoriaService auditoria;
    private final SessionRevocation sesiones;
    private final SecureRandom random = new SecureRandom();

    public CuentaMedicoService(UsuarioRepository usuarios, MedicoRepository medicos, RolRepository roles,
            BCryptPasswordEncoder passwords, AuditoriaService auditoria, SessionRevocation sesiones) {
        this.usuarios = usuarios; this.medicos = medicos; this.roles = roles;
        this.passwords = passwords; this.auditoria = auditoria; this.sesiones = sesiones;
    }

    public CredencialesMedico asegurarCuenta(Medico medico) {
        medicos.bloquearPorId(medico.getIdMedico()).orElseThrow();
        var existente = usuarios.findByMedico_IdMedico(medico.getIdMedico());
        if (existente.isPresent()) {
            medico.setUsuario(existente.get());
            return null;
        }
        Rol rol = roles.findByNombre("MEDICO").orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "No existe el rol MEDICO"));
        String base = "medico." + medico.getIdMedico();
        String username = base;
        int suffix = 1;
        while (usuarios.existsByUsername(username)) username = base + "." + suffix++;
        String password = nuevaPassword();
        Usuario user = new Usuario();
        user.setUsername(username);
        user.setPassword(passwords.encode(password));
        String nombre = (medico.getNombres() + " " + medico.getApellidoPaterno() + " "
                + (medico.getApellidoMaterno() == null ? "" : medico.getApellidoMaterno())).trim();
        user.setNombres(nombre.substring(0, Math.min(100, nombre.length())));
        user.setCorreo(medico.getCorreo());
        user.setRol(rol);
        user.setMedico(medico);
        user.setEstado(true);
        Usuario saved = usuarios.saveAndFlush(user);
        medico.setUsuario(saved);
        auditoria.registrarOperacion("INSERT", "Usuario", saved.getIdUsuario());
        return new CredencialesMedico(username, password);
    }

    public String cambiarUsername(Long medicoId, String username) {
        if (username == null || username.isBlank() || username.trim().length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario debe contener entre 1 y 50 caracteres");
        }
        medicos.bloquearPorId(medicoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Médico no encontrado"));
        Usuario cuenta = usuarios.findByMedico_IdMedico(medicoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El médico no tiene cuenta vinculada"));
        String nuevo = username.trim();
        if (nuevo.equals(cuenta.getUsername())) return nuevo;
        if (usuarios.existsByUsername(nuevo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya existe");
        }
        String anterior = cuenta.getUsername();
        cuenta.setUsername(nuevo);
        usuarios.saveAndFlush(cuenta);
        auditoria.registrarOperacion("UPDATE", "Usuario", cuenta.getIdUsuario());
        sesiones.usuario(anterior);
        return nuevo;
    }

    public CredencialesMedico restablecer(Long usuarioId) {
        Usuario user = usuarios.findById(usuarioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (user.getMedico() == null || !"MEDICO".equals(user.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccione la cuenta vinculada a un médico");
        }
        String password = nuevaPassword();
        user.setPassword(passwords.encode(password));
        usuarios.save(user);
        auditoria.registrarOperacion("UPDATE", "Usuario", usuarioId);
        sesiones.usuario(user.getUsername());
        return new CredencialesMedico(user.getUsername(), password);
    }

    public void desvincular(Medico medico) {
        usuarios.findByMedico_IdMedico(medico.getIdMedico()).ifPresent(user -> {
            user.setMedico(null);
            medico.setUsuario(null);
            user.setEstado(false);
            usuarios.saveAndFlush(user);
            sesiones.usuario(user.getUsername());
            auditoria.registrarOperacion("UPDATE", "Usuario", user.getIdUsuario());
        });
    }

    private String nuevaPassword() {
        // 12 caracteres aleatorios para cuentas nuevas; nunca se modifica una cuenta existente.
        byte[] bytes = new byte[9]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
