package com.tecsup.medicos_especialidades04.Repository;

import com.tecsup.medicos_especialidades04.Model.Medico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicoRepository extends JpaRepository<Medico, Long> {
}
