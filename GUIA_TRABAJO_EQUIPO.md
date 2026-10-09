# Asignación del trabajo - Módulo Médicos Especialidades

## Fase 1 - Relaciones Hibernate/JPA (todos)
| Integrante | Entidad | Archivo | Qué terminar |
|---|---|---|---|
| Angie | Medicos | `Model/Medico.java` | TERMINADO: `@OneToMany` con horarios y medico_especialidad + borrado en cascada (`orphanRemoval`) |
| Mayra | Especialidades | `Model/Especialidades.java` | Agregar `@OneToMany` con consultorios y medico_especialidad |
| Meliton | Horarios | `Model/HorarioAtencion.java` | TERMINADO: `@ManyToOne` medico (obligatorio, `nullable=false`) y consultorio (opcional), ambas con `FetchType.LAZY` y `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})` para evitar problemas de rendimiento (N+1) y serialización JSON |
| Luis | Consultorios | `Model/Consultorio.java` | Verificar `@ManyToOne` especialidad, `@OneToMany` con horarios |

En todos los casos probar CRUD de las relaciones y tener listas: entidades, script/estructura BD y prueba.

## Fase 2 - Módulos individuales
| Pregunta | Responsable | Estado actual | Qué falta |
|---|---|---|---|
| 2 - Auditoría | Mayra | Esqueleto creado: `Model/Auditoria.java`, `Repository/AuditoriaRepository.java`, `Service/AuditoriaService.java`, `Controller/AuditoriaController.java` | Registrar automáticamente cada operación (INSERT/UPDATE/DELETE) en `auditoria` desde los servicios: usuario, fecha, operación, entidad, ID. Mostrar registros en la BD. |
| 3 - Usuarios/Roles backend | Angie | TERMINADO: validaciones (username unico, campos obligatorios), BCrypt, password oculto en JSON, roles por defecto ADMINISTRADOR/MEDICO/RECEPCIONISTA, activar/desactivar (borrado logico) | Prueba manual contra la BD |
| 4 - Frontend usuarios/roles | Luis | Esqueleto creado: `templates/usuarios/*`, `templates/roles/*`, rutas en `HomeController`, funciones en `app.js`, enlaces en el sidebar | Conectar formularios con `/api/usuarios` y `/api/roles`, listar con tabla dinámica, editar y activar/desactivar sin tocar la BD. |
| 5 - Control de acceso por rol | Meliton | TERMINADO: `config/SecurityConfig.java` implementado (`SecurityFilterChain`, rutas por rol, login con redirección, logout, página de error 403) y dependencia `spring-boot-starter-security` activa en `pom.xml` | Falta: ocultar las opciones del sidebar (`templates/layout/sidebar.html`) según el rol autenticado (ej. que un MÉDICO no vea "Consultorios"/"Horarios", ni un RECEPCIONISTA vea "Médicos"). |

## Notas
- Todo el esqueleto compila con `BUILD SUCCESS` (Java 21).
- Cada archivo nuevo ya tiene `TODO` con el nombre del responsable para no perder el avance.
- Angie: Fase 1 y Pregunta 3 terminadas (10 commits). Backend de usuarios y roles listo para que Luis conecte su frontend.
