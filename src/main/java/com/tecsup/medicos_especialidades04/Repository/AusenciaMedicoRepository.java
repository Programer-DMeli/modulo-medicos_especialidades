package com.tecsup.medicos_especialidades04.Repository;

import com.tecsup.medicos_especialidades04.Model.AusenciaMedico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AusenciaMedicoRepository extends JpaRepository<AusenciaMedico, Long> {
    boolean existsByMedico_IdMedico(Long medicoId);

    @Query("select a from AusenciaMedico a join fetch a.medico order by a.inicio desc")
    List<AusenciaMedico> listarConMedico();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AusenciaMedico a where a.idAusencia = :id")
    Optional<AusenciaMedico> bloquearPorId(Long id);

    @Query("""
        select a from AusenciaMedico a join fetch a.medico
        where a.estado = 'ACTIVO' and a.inicio < :fin and a.fin > :inicio
        """)
    List<AusenciaMedico> buscarEnRango(LocalDateTime inicio, LocalDateTime fin);
}
