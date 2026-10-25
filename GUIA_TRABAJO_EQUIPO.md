# Asignación del trabajo - Módulo Médicos Especialidades

## Fase 1 - Relaciones Hibernate/JPA (todos)
| Integrante | Entidad | Archivo | Qué terminar |
|---|---|---|---|
| Angie | Medicos | `Model/Medico.java` | TERMINADO: `@OneToMany` con horarios y medico_especialidad + borrado en cascada (`orphanRemoval`) |
| Mayra | Especialidades | `Model/Especialidades.java` | TERMINADO: `@OneToMany` con consultorios y medico_especialidad; CRUD verificado |
| Meliton | Horarios | `Model/HorarioAtencion.java` | TERMINADO: `@ManyToOne` medico (obligatorio, `nullable=false`) y consultorio (opcional), ambas con `FetchType.LAZY` y `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})` para evitar problemas de rendimiento (N+1) y serialización JSON |
| Luis | Consultorios | `Model/Consultorio.java` | TERMINADO: `@ManyToOne` especialidad y `@OneToMany` con horarios; CRUD e inversa verificados |

En todos los casos probar CRUD de las relaciones y tener listas: entidades, script/estructura BD y prueba.

## Fase 2 - Módulos individuales
| Pregunta | Responsable | Estado actual | Qué falta |
|---|---|---|---|
| 2 - Auditoría | Mayra | TERMINADO: INSERT/UPDATE/DELETE desde servicios, principal autenticado y transacción compartida | Evidencia JDBC y JUnit en `docs/evidencias/mysql/` |
| 3 - Usuarios/Roles backend | Angie | TERMINADO: validaciones, BCrypt, password oculto, roles base, edición y estados; protección del último administrador y revocación de sesión | Pruebas contra MariaDB de XAMPP en `docs/evidencias/mysql/` |
| 4 - Frontend usuarios/roles | Luis | TERMINADO: formularios conectados, tablas dinámicas, edición, activar/desactivar y mensajes; CSRF en peticiones | Plantillas y API verificadas con MockMvc; JavaScript comprobado sintácticamente |
| 5 - Control de acceso por rol | Meliton | TERMINADO: SecurityFilterChain, login, logout, CSRF, permisos y sidebar por rol | Consultas de agenda disponibles para médicos; gestión administrativa restringida. Pruebas positivas y negativas |

## Notas
- Ver el alcance, la matriz de permisos y las instrucciones reproducibles en `docs/ENTREGA.md`.
- Se verificaron 58 casos contra MariaDB de XAMPP en una base aislada; los reportes JUnit se adjuntan en `docs/evidencias/mysql/`. Cada médico tiene una cuenta individual de rol MEDICO; el alta entrega sus credenciales y Usuarios permite regenerarlas. Solo ADMINISTRADOR gestiona el personal; MEDICO consulta solo su perfil, especialidades y agenda mediante usuarios.medico_id, sin crear, editar o eliminar. Una cuenta sin vínculo queda bloqueada.
- La base original conserva CMP duplicados que requieren revisión antes de instalar su índice único; no se borraron registros.
