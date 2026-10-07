package com.tecsup.medicos_especialidades04.Service;

import com.tecsup.medicos_especialidades04.Model.Auditoria;
import com.tecsup.medicos_especialidades04.Repository.AuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// TODO (Mayra): completar logica de registro automatico desde servicios/controllers
@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    public List<Auditoria> listar() {
        return auditoriaRepository.findAll();
    }

    // TODO (Mayra): llamar a registrar en ConsultorioService/EspecialidadesService/etc
    // en cada create/update/delete para cumplir la Pregunta 2
    public Auditoria registrarOperacion(String usuario, String operacion, String entidad, Long registroId) {
        Auditoria a = new Auditoria();
        a.setUsuario(usuario);
        a.setOperacion(operacion);
        a.setEntidad(entidad);
        a.setRegistroId(registroId);
        a.setFechaHora(java.time.LocalDateTime.now());
        return auditoriaRepository.save(a);
    }
}
