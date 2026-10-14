package com.tecsup.medicos_especialidades04.Repository;

import com.tecsup.medicos_especialidades04.Model.HorarioAtencion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {
    boolean existsByMedico_IdMedicoAndEspecialidad_IdEspecialidad(Long medicoId, Long especialidadId);
    boolean existsByConsultorio_Id(Long consultorioId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HorarioAtencion h where h.idHorario = :id")
    Optional<HorarioAtencion> bloquearPorId(Long id);

    @Query("""
        select h from HorarioAtencion h left join h.consultorio c
        where h.estado = 'ACTIVO' and h.vigenteDesde is not null
          and (h.medico.idMedico = :medicoId or (:consultorioId is not null and c.id = :consultorioId))
        """)
    List<HorarioAtencion> buscarPorRecursos(Long medicoId, Long consultorioId);

    @Query("""
        select h from HorarioAtencion h join fetch h.medico left join fetch h.consultorio
        where h.estado = 'ACTIVO' and h.vigenteDesde is not null
          and h.vigenteDesde <= :hasta and (h.vigenteHasta is null or h.vigenteHasta >= :desde)
        """)
    List<HorarioAtencion> buscarParaAgenda(LocalDate desde, LocalDate hasta);
}
