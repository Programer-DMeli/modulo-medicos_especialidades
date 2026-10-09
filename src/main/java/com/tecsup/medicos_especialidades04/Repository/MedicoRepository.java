package com.tecsup.medicos_especialidades04.Repository;

import com.tecsup.medicos_especialidades04.Model.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    Optional<Medico> findByNumeroDocumento(String numeroDocumento);

    Optional<Medico> findByCodigoMedico(String codigoMedico);
}
