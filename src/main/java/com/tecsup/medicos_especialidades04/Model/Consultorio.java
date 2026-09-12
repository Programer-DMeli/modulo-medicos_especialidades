package com.tecsup.medicos_especialidades04.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "consultorios")
public class Consultorio {

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

    public Consultorio() {
    }

    public Consultorio(
            String codigo,
            String nombre,
            Integer piso,
            String area,
            EstadoConsultorio estado) {

        this.codigo = codigo;
        this.nombre = nombre;
        this.piso = piso;
        this.area = area;
        this.estado = estado;
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
}