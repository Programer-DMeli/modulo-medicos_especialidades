# Asignación del trabajo - Módulo Médicos Especialidades

## Fase 1 - Relaciones Hibernate/JPA (todos)
| Integrante | Entidad | Archivo | Qué terminar |
|---|---|---|---|
| Angie | Médicos | `Model/Medico.java` | Agregar `@OneToMany` con horarios y medico_especialidad |
| Mayra | Especialidades | `Model/Especialidades.java` | Agregar `@OneToMany` con consultorios y medico_especialidad |
| Meliton | Horarios | `Model/HorarioAtencion.java` | Verificar `@ManyToOne` medico/consultorio |
| Luis | Consultorios | `Model/Consultorio.java` | Verificar `@ManyToOne` especialidad, `@OneToMany` con horarios |

En todos los casos probar CRUD de las relaciones y tener listas: entidades, script/estructura BD y prueba.

## Fase 2 - Módulos individuales
| Pregunta | Responsable | Estado actual | Qué falta |
|---|---|---|---|
| 2 - Auditoría | Mayra | Esqueleto creado: `Model/Auditoria.java`, `Repository/AuditoriaRepository.java`, `Service/AuditoriaService.java`, `Controller/AuditoriaController.java` | Registrar automáticamente cada operación (INSERT/UPDATE/DELETE) en `auditoria` desde los servicios: usuario, fecha, operación, entidad, ID. Mostrar registros en la BD. |
| 3 - Usuarios/Roles backend | Angie | Esqueleto creado: `Model/Usuario.java`, `Model/Rol.java`, repositorios, `Service/UsuarioService.java`, `Service/RolService.java`, `Controller/UsuarioController.java`, `Controller/RolController.java` | Completar validaciones, encriptar password (BCrypt), asegurar los roles ADMINISTRADOR/MEDICO/RECEPCIONISTA, activar/desactivar. Probar CRUD contra la BD. |
| 4 - Frontend usuarios/roles | Luis | Esqueleto creado: `templates/usuarios/*`, `templates/roles/*`, rutas en `HomeController`, funciones en `app.js`, enlaces en el sidebar | Conectar formularios con `/api/usuarios` y `/api/roles`, listar con tabla dinámica, editar y activar/desactivar sin tocar la BD. |
| 5 - Control de acceso por rol | Meliton | Archivo base `config/SecurityConfig.java` (pendiente) y dependencia `spring-boot-starter-security` comentada en `pom.xml` | Activar Spring Security, `SecurityFilterChain` para rutas por rol, login, redirección y ocultar opciones del sidebar según el rol autenticado. |

## Notas
- Todo el esqueleto compila con `BUILD SUCCESS` (Java 21).
- Cada archivo nuevo ya tiene `TODO` con el nombre del responsable para no perder el avance.
