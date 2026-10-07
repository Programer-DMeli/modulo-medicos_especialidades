package com.tecsup.medicos_especialidades04.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

// Pregunta 1 - Relaciones:
@Entity
@Table(name = "consultorios")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Consultorio {
    // Relacion inversa: un consultorio tiene muchos horarios de atencion
    @OneToMany(mappedBy = "consultorio")
    @com.fasterxml.jackson.annotation.JsonIgnore // evita recursion infinita en el JSON
    private List<HorarioAtencion> horarios = new ArrayList<>();


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Integer piso;

    @Column(nullable = false, length = 100)
    private String area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoConsultorio estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id")
    private Especialidades especialidad;


    public Consultorio() {
    }

    public Consultorio(String codigo, String nombre, Integer piso,
                       String area, EstadoConsultorio estado,
                       Especialidades especialidad) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.piso = piso;
        this.area = area;
        this.estado = estado;
        this.especialidad = especialidad;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getPiso() {
        return piso;
    }

    public void setPiso(Integer piso) {
        this.piso = piso;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public EstadoConsultorio getEstado() {
        return estado;
    }

    public void setEstado(EstadoConsultorio estado) {
        this.estado = estado;
    }

    public Especialidades getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(Especialidades especialidad) {
        this.especialidad = especialidad;
    }

    public List<HorarioAtencion> getHorarios() {
        return horarios;
    }

    public void setHorarios(List<HorarioAtencion> horarios) {
        this.horarios = horarios;
    }
}
