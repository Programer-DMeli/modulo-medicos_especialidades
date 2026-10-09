package com.tecsup.medicos_especialidades04.Repository;

import com.tecsup.medicos_especialidades04.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    java.util.Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
    @Query("select case when count(u) > 0 then true else false end from Usuario u where u.rol.idRol = :idRol")
    boolean existeRolEnUso(@Param("idRol") Long idRol);
}
