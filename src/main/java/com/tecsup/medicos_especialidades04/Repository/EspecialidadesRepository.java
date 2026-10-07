package com.tecsup.medicos_especialidades04.Repository;

import com.tecsup.medicos_especialidades04.Model.Especialidades;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EspecialidadesRepository extends JpaRepository<Especialidades, Long> {

    Optional<Especialidades> findByNombre(String nombre);

    Optional<Especialidades> findByCodigo(String codigo);
}
