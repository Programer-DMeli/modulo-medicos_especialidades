package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.*;
import com.tecsup.medicos_especialidades04.Repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Identidad obtenida exclusivamente de la cuenta autenticada, nunca de la petición. */
@Service
@Transactional(readOnly = true)
public class AlcanceMedicoService {
    public static final String SIN_VINCULO = "Tu cuenta MEDICO no tiene un médico vinculado. Contacta al administrador para vincularla. Las consultas personales están bloqueadas.";
    private final UsuarioRepository usuarios;
    private final EntityManager em;

    public AlcanceMedicoService(UsuarioRepository usuarios, EntityManager em) {
        this.usuarios = usuarios;
        this.em = em;
    }

    public boolean esMedico() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEDICO"));
    }

    public Long medicoId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return usuarios.findByUsername(auth.getName()).map(Usuario::getMedico).map(Medico::getIdMedico)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, SIN_VINCULO));
    }

    public void comprobarFiltro(Long id) {
        if (id != null && !medicoId().equals(id)) noEncontrado();
    }

    public void noEncontrado() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso no disponible para tu cuenta");
    }

    public List<Medico> medicos() {
        return em.createQuery("select m from Medico m where m.idMedico = :medico", Medico.class)
                .setParameter("medico", medicoId()).getResultList();
    }

    public List<MedicoEspecialidad> asignaciones() {
        return em.createQuery("select r from MedicoEspecialidad r where r.medico.idMedico = :medico", MedicoEspecialidad.class)
                .setParameter("medico", medicoId()).getResultList();
    }

    public List<Especialidades> especialidades() {
        return em.createQuery("select distinct r.especialidad from MedicoEspecialidad r where r.medico.idMedico = :medico", Especialidades.class)
                .setParameter("medico", medicoId()).getResultList();
    }

    public List<HorarioAtencion> horarios() {
        return em.createQuery("select h from HorarioAtencion h where h.medico.idMedico = :medico", HorarioAtencion.class)
                .setParameter("medico", medicoId()).getResultList();
    }

    public List<Consultorio> consultorios() {
        return em.createQuery("select distinct h.consultorio from HorarioAtencion h where h.medico.idMedico = :medico and h.consultorio is not null", Consultorio.class)
                .setParameter("medico", medicoId()).getResultList();
    }

    public List<AusenciaMedico> ausencias() {
        return em.createQuery("select a from AusenciaMedico a where a.medico.idMedico = :medico order by a.inicio desc", AusenciaMedico.class)
                .setParameter("medico", medicoId()).getResultList();
    }
}
