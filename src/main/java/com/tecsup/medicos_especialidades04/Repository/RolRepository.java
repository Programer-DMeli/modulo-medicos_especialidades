package com.tecsup.medicos_especialidades04.Repository;

import com.tecsup.medicos_especialidades04.Model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> {
    // TODO (Angie): agregar metodos de busqueda si los requiere
}
