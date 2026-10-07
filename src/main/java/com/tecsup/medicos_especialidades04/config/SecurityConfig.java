package com.tecsup.medicos_especialidades04.config;

// TODO (Meliton - Pregunta 5): implementar con Spring Security
// 1. Agregar en pom.xml la dependencia spring-boot-starter-security (comentada arriba)
// 2. Crear clase @Configuration @EnableWebSecurity con SecurityFilterChain
// 3. Restringir rutas por rol:
//    - ADMINISTRADOR: /usuarios/**, /roles/**
//    - MEDICO: /medicos/**, /api/medicos/**
//    - RECEPCIONISTA: /consultorios/**, /horarios/**
// 4. Ocultar opciones del sidebar segun el rol en las plantillas (th:if sec:authorize)
// 5. Redirigir al login y bloquear accesos no permitidos (403)

public class SecurityConfig {
    // esqueleto pendiente de completar por Meliton
}
