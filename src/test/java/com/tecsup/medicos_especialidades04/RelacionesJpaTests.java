package com.tecsup.medicos_especialidades04;

import com.tecsup.medicos_especialidades04.Model.Consultorio;
import com.tecsup.medicos_especialidades04.Model.Especialidades;
import com.tecsup.medicos_especialidades04.Model.EstadoConsultorio;
import com.tecsup.medicos_especialidades04.Model.EstadoMedico;
import com.tecsup.medicos_especialidades04.Model.HorarioAtencion;
import com.tecsup.medicos_especialidades04.Model.Medico;
import com.tecsup.medicos_especialidades04.Model.MedicoEspecialidad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pruebas CRUD de las relaciones JPA (Fase 1).
 * Se ejecutan contra la base de datos MySQL real (medico_especialidad),
 * con rollback automatico al terminar cada prueba. Cada prueba genera sus
 * propios datos unicos para no interferir con los datos reales de la BD.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RelacionesJpaTests {

    @Autowired
    private TestEntityManager em;

    /** Sufijo unico para que los datos de prueba no colisionen con datos reales. */
    private String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Medico crearMedico(String codigo, String numeroDocumento) {
        Medico medico = new Medico();
        medico.setCodigoMedico(codigo);
        medico.setTipoDocumento("DNI");
        medico.setNumeroDocumento(numeroDocumento);
        medico.setNombres("Carlos");
        medico.setApellidoPaterno("Ramirez");
        medico.setApellidoMaterno("Torres");
        medico.setCmp("CMP-001");
        medico.setFechaIngreso(LocalDate.of(2026, 1, 10));
        medico.setEstado(EstadoMedico.ACTIVO);
        return medico;
    }

    @Test
    void crudConsultorioConEspecialidad() {
        String sufijo = uid();
        String codigoEspecialidad = "CARD-" + sufijo;
        String nombreEspecialidad = "Cardiologia " + sufijo;

        // CREATE
        Especialidades especialidad =
                new Especialidades(codigoEspecialidad, nombreEspecialidad, "Corazon", 30, true);
        em.persist(especialidad);

        Consultorio consultorio = new Consultorio(
                "CON-" + sufijo, "Consultorio 1", 1, "Piso clinico",
                EstadoConsultorio.DISPONIBLE, especialidad);
        em.persist(consultorio);
        em.flush();
        em.clear();

        // READ - lado ManyToOne
        Consultorio leido = em.find(Consultorio.class, consultorio.getId());
        assertNotNull(leido.getEspecialidad());
        assertEquals(nombreEspecialidad, leido.getEspecialidad().getNombre());

        // READ - lado OneToMany inverso
        Especialidades especialidadLeida = em.find(Especialidades.class, especialidad.getIdEspecialidad());
        assertEquals(1, especialidadLeida.getConsultorios().size());

        // UPDATE
        leido.setNombre("Consultorio Cardiologia");
        em.merge(leido);
        em.flush();
        em.clear();
        assertEquals("Consultorio Cardiologia",
                em.find(Consultorio.class, consultorio.getId()).getNombre());

        // DELETE
        em.remove(em.find(Consultorio.class, consultorio.getId()));
        em.flush();
        em.clear();
        assertNull(em.find(Consultorio.class, consultorio.getId()));
        assertEquals(0,
                em.find(Especialidades.class, especialidad.getIdEspecialidad()).getConsultorios().size());
    }

    @Test
    void crudHorarioConMedicoYConsultorio() {
        String sufijo = uid();
        String codigoMedico = "MED-" + sufijo;

        Medico medico = crearMedico(codigoMedico, "9" + sufijo);
        em.persist(medico);
        // Consultorio sin especialidad: la relacion es opcional en la entidad
        Consultorio consultorio = new Consultorio(
                "CON-" + sufijo, "Consultorio 2", 2, "Traumatologia",
                EstadoConsultorio.DISPONIBLE, null);
        em.persist(consultorio);

        HorarioAtencion horario = new HorarioAtencion();
        horario.setMedico(medico);
        horario.setConsultorio(consultorio);
        horario.setDiaSemana("LUNES");
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(12, 0));
        horario.setDuracionCita(30);
        horario.setEstado("ACTIVO");
        em.persist(horario);
        em.flush();
        em.clear();

        // READ - relaciones LAZY medico (obligatorio) y consultorio (opcional)
        HorarioAtencion leido = em.find(HorarioAtencion.class, horario.getIdHorario());
        assertNotNull(leido.getMedico());
        assertEquals(codigoMedico, leido.getMedico().getCodigoMedico());
        assertEquals("Consultorio 2", leido.getConsultorio().getNombre());

        // READ - OneToMany desde medico
        assertEquals(1, em.find(Medico.class, medico.getIdMedico()).getHorarios().size());

        // UPDATE
        leido.setHoraFin(LocalTime.of(14, 0));
        em.merge(leido);
        em.flush();
        em.clear();
        assertEquals(LocalTime.of(14, 0),
                em.find(HorarioAtencion.class, horario.getIdHorario()).getHoraFin());

        // DELETE
        em.remove(em.find(HorarioAtencion.class, horario.getIdHorario()));
        em.flush();
        em.clear();
        assertNull(em.find(HorarioAtencion.class, horario.getIdHorario()));
    }

    @Test
    void medicoSeRelacionaConEspecialidad() {
        String sufijo = uid();
        String nombreEspecialidad = "Neurologia " + sufijo;

        Medico medico = crearMedico("MED-" + sufijo, "8" + sufijo);
        em.persist(medico);
        Especialidades especialidad =
                new Especialidades("NEUR-" + sufijo, nombreEspecialidad, "Sistema nervioso", 45, true);
        em.persist(especialidad);

        MedicoEspecialidad asignacion = new MedicoEspecialidad(medico, especialidad, "Epilepsia");
        em.persist(asignacion);
        em.flush();
        em.clear();

        // READ - la tabla puente resuelve la relacion muchos a muchos
        MedicoEspecialidad leida = em.find(MedicoEspecialidad.class, asignacion.getIdMedicoEspecialidad());
        assertEquals("Carlos", leida.getMedico().getNombres());
        assertEquals(nombreEspecialidad, leida.getEspecialidad().getNombre());

        assertEquals(1, em.find(Medico.class, medico.getIdMedico()).getMedicoEspecialidades().size());
        assertEquals(1,
                em.find(Especialidades.class, especialidad.getIdEspecialidad()).getMedicoEspecialidades().size());

        // DELETE
        em.remove(em.find(MedicoEspecialidad.class, asignacion.getIdMedicoEspecialidad()));
        em.flush();
        em.clear();
        assertEquals(0, em.find(Medico.class, medico.getIdMedico()).getMedicoEspecialidades().size());
    }

    @Test
    void quitarHorarioDeLaColeccionDeMedicoLoEliminaPorOrphanRemoval() {
        String sufijo = uid();

        Medico medico = crearMedico("MED-" + sufijo, "7" + sufijo);
        em.persist(medico);

        HorarioAtencion horario = new HorarioAtencion();
        horario.setMedico(medico);
        horario.setDiaSemana("MARTES");
        horario.setHoraInicio(LocalTime.of(9, 0));
        horario.setHoraFin(LocalTime.of(13, 0));
        horario.setDuracionCita(20);
        horario.setEstado("ACTIVO");
        em.persist(horario);
        em.flush();
        em.clear();

        Medico medicoLeido = em.find(Medico.class, medico.getIdMedico());
        assertEquals(1, medicoLeido.getHorarios().size());

        // orphanRemoval = true: sacarlo de la coleccion lo borra de la BD
        medicoLeido.getHorarios().clear();
        em.flush();
        em.clear();

        assertNull(em.find(HorarioAtencion.class, horario.getIdHorario()));
        assertEquals(0, em.find(Medico.class, medico.getIdMedico()).getHorarios().size());
    }
}
