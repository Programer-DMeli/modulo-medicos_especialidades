package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Auditoria;
import com.tecsup.medicos_especialidades04.Repository.AuditoriaRepository;
import com.tecsup.medicos_especialidades04.config.AuditUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    public List<Auditoria> listar() {
        return auditoriaRepository.findAll();
    }

    public Auditoria registrarOperacion(String operacion, String entidad, Long registroId) {
        String usuario = AuditUtil.getCurrentUsername();
        Auditoria a = new Auditoria();
        a.setUsuario(usuario);
        a.setOperacion(operacion);
        a.setEntidad(entidad);
        a.setRegistroId(registroId);
        a.setFechaHora(java.time.LocalDateTime.now());
        return auditoriaRepository.save(a);
    }
}
