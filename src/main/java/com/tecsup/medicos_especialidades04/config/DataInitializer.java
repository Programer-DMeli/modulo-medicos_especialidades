package com.tecsup.medicos_especialidades04.config;

import com.tecsup.medicos_especialidades04.Model.Rol;
import com.tecsup.medicos_especialidades04.Repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private RolRepository rolRepository;

    @Override
    public void run(ApplicationArguments args) {
        crearRolSiNoExiste("ADMINISTRADOR", "Acceso total al sistema");
        crearRolSiNoExiste("MEDICO", "Gestiona pacientes e historias clinicas");
        crearRolSiNoExiste("RECEPCIONISTA", "Gestiona pacientes y citas");
    }

    private void crearRolSiNoExiste(String nombre, String descripcion) {
        if (!rolRepository.existsByNombre(nombre)) {
            Rol rol = new Rol();
            rol.setNombre(nombre);
            rol.setDescripcion(descripcion);
            rolRepository.save(rol);
        }
    }
}
