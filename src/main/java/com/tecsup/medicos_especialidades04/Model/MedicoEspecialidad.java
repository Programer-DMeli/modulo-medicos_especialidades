package com.tecsup.medicos_especialidades04.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "medico_especialidad")
public class MedicoEspecialidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medico_especialidad")
    private Long idMedicoEspecialidad;

    @ManyToOne
    @JoinColumn(name = "id_medico", nullable = false)
    private Medico medico;

    @ManyToOne
    @JoinColumn(name = "id_especialidad", nullable = false)
    private Especialidades especialidad;

    @Column(name = "subespecialidad", length = 100)
    private String subespecialidad;

    public MedicoEspecialidad() {
    }

    public MedicoEspecialidad(
            Medico medico,
            Especialidades especialidad,
            String subespecialidad) {

        this.medico = medico;
        this.especialidad = especialidad;
        this.subespecialidad = subespecialidad;
    }

    public Long getIdMedicoEspecialidad() {
        return idMedicoEspecialidad;
    }

    public void setIdMedicoEspecialidad(Long idMedicoEspecialidad) {
        this.idMedicoEspecialidad = idMedicoEspecialidad;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public Especialidades getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(Especialidades especialidad) {
        this.especialidad = especialidad;
    }

    public String getSubespecialidad() {
        return subespecialidad;
    }

    public void setSubespecialidad(String subespecialidad) {
        this.subespecialidad = subespecialidad;
    }
}
