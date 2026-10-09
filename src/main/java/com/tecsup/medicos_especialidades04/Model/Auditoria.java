package com.tecsup.medicos_especialidades04.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Pregunta 2: Auditoria - Mayra
@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "usuario", length = 100)
    private String usuario;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(name = "operacion", length = 20)
    private String operacion; // INSERT, UPDATE, DELETE

    @Column(name = "entidad", length = 50)
    private String entidad;

    @Column(name = "registro_id")
    private Long registroId;

    // TODO (Mayra): registrar campos adicionales si los pide la rubrica

    public Auditoria() {
    }

    public Long getIdAuditoria() { return idAuditoria; }
    public void setIdAuditoria(Long idAuditoria) { this.idAuditoria = idAuditoria; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public String getOperacion() { return operacion; }
    public void setOperacion(String operacion) { this.operacion = operacion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public Long getRegistroId() { return registroId; }
    public void setRegistroId(Long registroId) { this.registroId = registroId; }
}
