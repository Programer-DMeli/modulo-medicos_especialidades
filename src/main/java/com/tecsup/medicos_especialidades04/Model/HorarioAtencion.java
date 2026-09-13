package com.tecsup.medicos_especialidades04.Model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "horario_atencion")
public class HorarioAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Long idHorario;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @ManyToOne
    @JoinColumn(name = "consultorio_id")
    private Consultorio consultorio;

    @Column(name = "dia_semana", nullable = false, length = 20)
    private String diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "duracion_cita", nullable = false)
    private Integer duracionCita;

    @Column(name = "estado", nullable = false)
    private String estado;

    public HorarioAtencion() {
    }

    // getters y setters

    public Long getIdHorario() {
        return idHorario;
    }

    public Medico getMedico() {
        return medico;
    }

    public Consultorio getConsultorio() {
        return consultorio;
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public Integer getDuracionCita() {
        return duracionCita;
    }

    public String getEstado() {
        return estado;
    }

    public void setIdHorario(Long idHorario) {
        this.idHorario = idHorario;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public void setConsultorio(Consultorio consultorio) {
        this.consultorio = consultorio;
    }

    public void setDiaSemana(String diaSemana) {
        this.diaSemana = diaSemana;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }
    public void setDuracionCita(Integer duracionCita) {
        this.duracionCita = duracionCita;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }
}
