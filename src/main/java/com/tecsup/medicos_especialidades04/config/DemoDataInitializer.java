package com.tecsup.medicos_especialidades04.config;

import com.tecsup.medicos_especialidades04.Model.*;
import com.tecsup.medicos_especialidades04.Repository.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Fictional, disposable data, exclusively for the explicitly selected demo profile. */
@Component
@Profile("demo")
@Order(1)
public class DemoDataInitializer implements ApplicationRunner {
    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final MedicoRepository medicos;
    private final EspecialidadesRepository especialidades;
    private final ConsultorioRepository consultorios;
    private final MedicoEspecialidadRepository asignaciones;
    private final HorarioAtencionRepository horarios;
    private final AusenciaMedicoRepository ausencias;
    private final BCryptPasswordEncoder passwords;

    public DemoDataInitializer(RolRepository roles, UsuarioRepository usuarios, MedicoRepository medicos,
            EspecialidadesRepository especialidades, ConsultorioRepository consultorios,
            MedicoEspecialidadRepository asignaciones, HorarioAtencionRepository horarios,
            AusenciaMedicoRepository ausencias, BCryptPasswordEncoder passwords) {
        this.roles = roles;
        this.usuarios = usuarios;
        this.medicos = medicos;
        this.especialidades = especialidades;
        this.consultorios = consultorios;
        this.asignaciones = asignaciones;
        this.horarios = horarios;
        this.ausencias = ausencias;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) return;
        Map<String, String> accounts = Map.of("admin.demo", "ADMINISTRADOR",
                "recepcion.demo", "RECEPCIONISTA", "medico.demo", "MEDICO");
        accounts.forEach((username, role) -> {
            Usuario user = new Usuario();
            user.setUsername(username);
            user.setNombres("Cuenta de demostración " + role);
            user.setCorreo(username + "@example.test");
            user.setPassword(passwords.encode("Demo2026!"));
            user.setRol(roles.findByNombre(role).orElseThrow());
            usuarios.save(user);
        });
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String[] days = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        String[] names = {"Lucía", "Diego", "Valeria"};
        String[] lastNames = {"Salazar", "Rojas", "Mendoza"};
        String[] specialties = {"Cardiología", "Pediatría", "Medicina general"};
        for (int index = 0; index < names.length; index++) {
            Especialidades specialty = especialidades.save(new Especialidades("ESP-DEMO-" + index,
                    specialties[index], "Especialidad ficticia para demostración", 30, true));
            Consultorio room = consultorios.save(new Consultorio("CON-DEMO-" + index,
                    "Consultorio " + (index + 101), 1, "Atención ambulatoria", EstadoConsultorio.DISPONIBLE, specialty));
            Medico doctor = new Medico();
            doctor.setCodigoMedico("MED-DEMO-" + index);
            doctor.setTipoDocumento("DNI");
            doctor.setNumeroDocumento("0000000" + index);
            doctor.setNombres(names[index]);
            doctor.setApellidoPaterno(lastNames[index]);
            doctor.setApellidoMaterno("Demo");
            doctor.setCmp("DEMO-" + index);
            doctor.setCorreo("doctor" + index + "@example.test");
            doctor.setTelefono("90000000" + index);
            doctor.setFechaIngreso(today.minusYears(1));
            doctor.setEstado(EstadoMedico.ACTIVO);
            doctor = medicos.save(doctor);
            asignaciones.save(new MedicoEspecialidad(doctor, specialty, ""));
            for (String day : days) {
                HorarioAtencion schedule = new HorarioAtencion();
                schedule.setMedico(doctor);
                schedule.setConsultorio(room);
                schedule.setEspecialidad(specialty);
                schedule.setDiaSemana(day);
                schedule.setHoraInicio(LocalTime.of(index == 1 ? 14 : 8, 0));
                schedule.setHoraFin(LocalTime.of(index == 1 ? 18 : 12, 0));
                schedule.setDuracionCita(30);
                schedule.setEstado("ACTIVO");
                schedule.setVigenteDesde(monday.minusWeeks(2));
                schedule.setVigenteHasta(monday.plusWeeks(12).plusDays(6));
                horarios.save(schedule);
            }
            if (index == 2) {
                AusenciaMedico absence = new AusenciaMedico();
                absence.setMedico(doctor);
                absence.setInicio(monday.plusDays(2).atTime(9, 0));
                absence.setFin(monday.plusDays(2).atTime(11, 0));
                absence.setMotivo("Capacitación de demostración");
                absence.setEstado("ACTIVO");
                ausencias.save(absence);
            }
        }
    }
}
