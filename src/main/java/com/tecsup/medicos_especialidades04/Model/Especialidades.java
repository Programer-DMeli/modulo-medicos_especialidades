package com.tecsup.medicos_especialidades04.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "especialidad")
public class Especialidades {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especialidad")
    private Long idEspecialidad;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "duracion_consulta", nullable = false)
    private Integer duracionConsulta;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    public Especialidades() {
    }

    public Especialidades(
            String codigo,
            String nombre,
            String descripcion,
            Integer duracionConsulta,
            Boolean estado) {

        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionConsulta = duracionConsulta;
        this.estado = estado;
    }

    public Long getIdEspecialidad() {
        return idEspecialidad;
    }

    public void setIdEspecialidad(Long idEspecialidad) {
        this.idEspecialidad = idEspecialidad;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getDuracionConsulta() {
        return duracionConsulta;
    }

    public void setDuracionConsulta(Integer duracionConsulta) {
        this.duracionConsulta = duracionConsulta;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }
}