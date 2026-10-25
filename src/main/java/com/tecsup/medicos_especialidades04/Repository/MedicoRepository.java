package com.tecsup.medicos_especialidades04.Repository;

import com.tecsup.medicos_especialidades04.Model.Medico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    Optional<Medico> findByNumeroDocumento(String numeroDocumento);

    Optional<Medico> findByCodigoMedico(String codigoMedico);

    boolean existsByCmp(String cmp);

    boolean existsByCmpAndIdMedicoNot(String cmp, Long idMedico);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medico m where m.idMedico = :id")
    Optional<Medico> bloquearPorId(Long id);
}
