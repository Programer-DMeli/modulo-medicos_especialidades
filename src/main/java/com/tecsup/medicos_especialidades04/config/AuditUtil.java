package com.tecsup.medicos_especialidades04.config;

public class AuditUtil {

    public static String getCurrentUsername() {
        // Spring Security no está configurado aún, retorna usuario por defecto
        // TODO: Implementar cuando se configure Spring Security (Pregunta 5)
        return "SYSTEM";
    }
}