package com.tecsup.medicos_especialidades04.config;

import com.tecsup.medicos_especialidades04.Repository.MedicoRepository;
import com.tecsup.medicos_especialidades04.Service.CuentaMedicoService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(2)
public class CuentasMedicosInitializer implements ApplicationRunner {
    private final MedicoRepository medicos;
    private final CuentaMedicoService cuentas;
    public CuentasMedicosInitializer(MedicoRepository medicos, CuentaMedicoService cuentas) {
        this.medicos = medicos; this.cuentas = cuentas;
    }
    public void run(ApplicationArguments args) {
        medicos.findAll().forEach(cuentas::asegurarCuenta);
        // El administrador entrega una contraseña nueva desde Usuarios.
        // No se vuelven a crear cuentas ni se reactivan usuarios deshabilitados.
    }
}
