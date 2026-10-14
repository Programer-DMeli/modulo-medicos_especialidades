package com.tecsup.medicos_especialidades04;

import com.tecsup.medicos_especialidades04.Model.*;
import com.tecsup.medicos_especialidades04.Repository.AuditoriaRepository;
import com.tecsup.medicos_especialidades04.Service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "evaluador", roles = "ADMINISTRADOR")
class AuditoriaCrudTests {
    @Autowired private MedicoService medicos;
    @Autowired private EspecialidadesService especialidades;
    @Autowired private MedicoEspecialidadService relaciones;
    @Autowired private AuditoriaRepository auditoria;

    @Test
    void especialidadRegistraAltaCambioYBorradoConUsuarioReal() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Especialidades especialidad = especialidades.guardar(new Especialidades("ESP-" + suffix,
                "Especialidad " + suffix, "Prueba de auditoría", 30, true));
        Long id = especialidad.getIdEspecialidad();
        especialidades.cambiarEstado(id, false);
        especialidades.eliminar(id);
        comprobarOperaciones("Especialidad", id);
    }

    @Test
    void medicoConservaValidacionDeDuplicadosYAuditaLasTresOperaciones() {
        Medico medico = medicos.guardar(nuevoMedico());
        Long id = medico.getIdMedico();
        Medico duplicado = nuevoMedico();
        duplicado.setNumeroDocumento(medico.getNumeroDocumento());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> medicos.guardar(duplicado));
        medico.setTelefono("999000111");
        medicos.guardar(medico);
        medicos.eliminar(id);
        comprobarOperaciones("Medico", id);
    }

    @Test
    void relacionMedicoEspecialidadPermiteCrudYAudita() {
        Medico medico = medicos.guardar(nuevoMedico());
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Especialidades especialidad = especialidades.guardar(new Especialidades("ESP-" + suffix,
                "Especialidad " + suffix, "Relaciones JPA", 30, true));
        MedicoEspecialidad relacion = relaciones.guardar(new MedicoEspecialidad(medico, especialidad, "Consulta inicial"));
        Long id = relacion.getIdMedicoEspecialidad();
        assertEquals(especialidad.getIdEspecialidad(), relaciones.obtener(id).getEspecialidad().getIdEspecialidad());
        relacion.setSubespecialidad("Consulta de seguimiento");
        relaciones.guardar(relacion);
        assertEquals("Consulta de seguimiento", relaciones.obtener(id).getSubespecialidad());
        relaciones.eliminar(id);
        assertNull(relaciones.obtener(id));
        comprobarOperaciones("MedicoEspecialidad", id);
    }

    private void comprobarOperaciones(String entidad, Long id) {
        List<Auditoria> registros = auditoria.findAll().stream()
                .filter(item -> entidad.equals(item.getEntidad()) && id.equals(item.getRegistroId())).toList();
        assertEquals(List.of("INSERT", "UPDATE", "DELETE"), registros.stream().map(Auditoria::getOperacion).toList());
        registros.forEach(item -> {
            assertEquals("evaluador", item.getUsuario());
            assertNotNull(item.getFechaHora());
        });
    }

    private Medico nuevoMedico() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Medico medico = new Medico();
        medico.setCodigoMedico("MED-" + suffix);
        medico.setTipoDocumento("DNI");
        medico.setNumeroDocumento(String.format("%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100000000)));
        medico.setNombres("Médico de prueba");
        medico.setApellidoPaterno("Auditoría");
        medico.setApellidoMaterno("JPA");
        medico.setCmp("CMP-" + suffix);
        medico.setTelefono("999000111");
        Especialidades base = especialidades.guardar(new Especialidades(null, "Base " + suffix, "", 30, true));
        medico.setEspecialidadIds(List.of(base.getIdEspecialidad()));
        medico.setFechaIngreso(LocalDate.now());
        medico.setEstado(EstadoMedico.ACTIVO);
        return medico;
    }
}
