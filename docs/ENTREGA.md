# Verificación de entrega

**Ejecución actual:** perfil `local`, MariaDB de XAMPP y base `medico_especialidad`. Véase [CONEXION_LOCAL.md](CONEXION_LOCAL.md). La demo de revisiones anteriores ya no está activa.

Rama: `feature/test_integration`. Validación del panel personal realizada el 21 de octubre de 2026.

## Fase 1: relaciones

| Responsable | Relación | Implementación y evidencia |
|---|---|---|
| Angie | Médico → horarios y médico/especialidad | `Medico.java`: dos `@OneToMany(mappedBy="medico")`. CRUD e inversas en `RelacionesJpaTests`. |
| Mayra | Especialidad → consultorios y médico/especialidad | `Especialidades.java`: dos `@OneToMany(mappedBy="especialidad")`. Pruebas de lectura, actualización y borrado. |
| Meliton | Horario → médico y consultorio | `HorarioAtencion.java`: `@ManyToOne` y `@JoinColumn`; médico obligatorio, consultorio opcional. |
| Luis | Consultorio → especialidad y horarios | `Consultorio.java`: `@ManyToOne` y `@OneToMany(mappedBy="consultorio")`. CRUD, inversa y auditoría verificados. |

Las relaciones ya estaban implementadas; se amplió la comprobación de la colección inversa de consultorios y de la actualización de la tabla puente. `docs/schema.sql` define las claves foráneas. La salida real de `information_schema.KEY_COLUMN_USAGE` se adjunta en `evidencias/mysql/resultado.txt`.

## Fase 2: módulos

| Módulo | Resultado |
|---|---|
| Auditoría | Los servicios registran usuario autenticado, fecha, operación, entidad e ID en la misma transacción. INSERT/UPDATE/DELETE comprobados; fallos de auditoría revierten la operación. |
| Usuarios y roles | BCrypt; contraseña excluida del JSON; nombres únicos; roles base; edición y activar/desactivar; protección del último administrador; revocación de sesiones. Se agregaron límites de longitud, validación de correo y límite BCrypt de 72 bytes UTF-8. |
| Frontend | Formularios conectados a `/api/usuarios` y `/api/roles`, tablas dinámicas, edición y estados. Se reemplazó el botón engañoso «Eliminar usuario» por activar/desactivar, ya que el backend conserva el registro. Los roles base no muestran borrado. |
| Seguridad | Spring Security activo, login, CSRF, permisos y menú por rol. Se protegió la página de consultorios con los mismos roles que su menú. Las plantillas administrativas se prueban para administrador y recepción. |

### Permisos del panel personal (21 de octubre de 2026)

| Recurso | MEDICO | RECEPCIONISTA | ADMINISTRADOR |
|---|---|---|---|
| Médicos | Mi perfil: solo la ficha vinculada, consulta | Directorio, consulta | Gestión completa |
| Especialidades | Mis especialidades: todas las asignadas, consulta | Catálogo, consulta | Gestión completa |
| Horarios, agenda y ausencias | Solo registros propios, consulta | Consulta y programación general | Consulta y programación general |
| Consultorios | API: solo los asignados mediante sus horarios | Gestión | Gestión |
| Usuarios, roles y auditoría | Sin acceso | Sin acceso | Permisos administrativos existentes |

`AlcanceMedicoService` resuelve la identidad desde el nombre de la sesión y `usuarios.medico_id`. Los servicios de lectura consultan únicamente filas de ese médico; nunca usan un ID del navegador como identidad. Los detalles ajenos o inexistentes devuelven 404. La agenda rechaza filtros de médico o consultorio ajenos; los parámetros de búsqueda adicionales no amplían los listados. Inicio consume estas mismas APIs restringidas.

Una cuenta MEDICO sin vínculo recibe 403 en las APIs y una página explicativa sin cargar consultas personales. No hay alternativa con información general. El panel es de consulta y conserva el bloqueo de escritura en Spring Security, además de ocultar los controles de gestión. Las relaciones inversas de médicos, especialidades y consultorios siguen excluidas del JSON: un recurso compartido no expone otros médicos ni sus horarios.

No se alteran la generación automática de cuentas/credenciales, contraseñas ni estados existentes. El perfil `demo` enlaza exclusivamente sus datos nuevos y desechables para facilitar la revisión con dos cuentas.

`PanelMedicoTests` verifica ambas cuentas, múltiples especialidades, recursos compartidos, IDs ajenos, filtros manipulados, consultas directas, ausencia de credenciales/relaciones inversas, bloqueo de escritura, cuenta sin vínculo y permisos de administrador/recepción. La suite existente mantiene las comprobaciones de creación de cuentas, login, programación, auditoría y concurrencia.

## Evidencia reproducible

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
.\mvnw.cmd test
.\scripts\verificar-mysql.ps1
```

El segundo comando de verificación usa una base nueva en MariaDB de XAMPP. Las pruebas transaccionales revierten sus fixtures; las filas `EVIDENCIA_BD` se leen por JDBC antes del rollback y se guardan como evidencia textual. Los reportes JUnit documentan los casos y las aserciones. La comprobación HTTP y MockMvc cubre las páginas, API, formularios, login y autorizaciones; no equivale a una prueba manual completa en un navegador.

## Ejecución y datos existentes

### Cuentas individuales de médicos

Cada alta realizada mediante `MedicoService` crea en la misma transacción una cuenta activa de rol `MEDICO` vinculada mediante `usuarios.medico_id`. La relación es única, no depende del CMP y permite incorporar también médicos heredados con datos duplicados. El arranque sincroniza los médicos sin cuenta; conserva las cuentas genéricas existentes y las contraseñas/estados de cuentas ya vinculadas.

El alta devuelve usuario y contraseña nueva una sola vez; el frontend los muestra antes de continuar. No se incluyen en las consultas de médicos ni de usuarios, ni se guardan en logs o almacenamiento del navegador. En Usuarios, la columna Médico identifica el registro y el botón Generar credenciales permite al administrador restablecer una contraseña y entregarla. Requiere sesión de administrador y CSRF, registra auditoría y revoca sesiones anteriores. No reactiva cuentas deshabilitadas.

Una cuenta vinculada debe conservar el rol MEDICO. Al eliminar un médico sin historial, se desvincula y desactiva su cuenta, conservando el usuario y la auditoría. El estado clínico (suspendido/vacaciones) no equivale a desactivar el acceso; esa decisión sigue disponible en Usuarios. La migración necesaria es `003_cuentas_medicos.sql`.

`CuentasMedicosTests` verifica alta con BCrypt y login, sincronización idempotente, entrega única, restablecimiento exclusivo para administrador y desactivación al borrar. La suite ampliada tiene 58 casos: todos pasaron en H2 2.3.232 y MariaDB 10.4.32 de XAMPP (base aislada `medicos_integracion_b23b46b0`). Los reportes están en `evidencias/h2/` y `evidencias/mysql/`.

```powershell
.\mvnw.cmd spring-boot:run
```

Abra `http://localhost:8080/login`. Use sus usuarios y contraseñas existentes; no se restablecieron cuentas. La conexión local usa `root` sin contraseña y el dialecto de MariaDB. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DIALECT` y `PORT` permiten otro entorno. El perfil `demo` tiene cuentas ficticias documentadas en `migrations/README.md`.

La base de trabajo conserva dos médicos con CMP `3456`. Por ese motivo no se instaló el índice único de CMP; el servicio ahora comprueba existencia sin fallar por múltiples resultados y rechaza conflictos. Deben revisarse esos dos registros antes de instalar ese índice de `002_especialidad_medico.sql`. No se modificaron sus identificadores ni se eliminaron datos. El respaldo previo está en `target/respaldo-medico-especialidad-antes-arranque.sql` y no se incorpora a Git.

### Revisión aislada del panel personal

En la revisión anterior se usó el perfil `demo` en `http://localhost:8081/login`, con H2 en memoria. Cuentas: `medico.demo` (Lucía), `medico2.demo` (Diego), `admin.demo` y `recepcion.demo`; contraseña de demostración: `Demo2026!`. Estos datos son ficticios y se pierden al detener la instancia.

```powershell
& 'C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe' -jar target/medicos_especialidades04-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --server.port=8081
```

No se ejecutaron migraciones ni operaciones sobre la base de trabajo para esta entrega. El trabajo local anterior a la integración remota se conserva además en `stash@{0}`, identificado como `respaldo-panel-medico-antes-integracion-remoto-2026-10-21`.

### Integración del remoto (21 de octubre de 2026)

La rama `feature/test_integration` se actualizó por fast-forward hasta `29187e6`, incorporando los cuatro commits pendientes. Los cambios locales se reaplicaron y los conflictos se resolvieron sin eliminar el panel personal ni las credenciales automáticas.

- Frontend: cabeceras azules, filas de auditoría resaltadas y estados accionables desde tablas. Se unificaron los controles para evitar botones duplicados, manteniendo la confirmación al desactivar usuarios/roles y la opción de generar credenciales.
- Backend seleccionado: endpoints de cambio de estado para médicos, consultorios y horarios, con autorización existente, auditoría, bloqueo transaccional y validaciones de estados/cruces. No requieren migraciones.
- Adaptación necesaria: cambiar el estado de un horario iniciado crea una versión desde mañana, usando `actualizarBloqueado`; conserva la versión histórica. Los horarios terminados no admiten cambios.
- Se descartó la restricción remota que oculta y bloquea `/medicos` para MEDICO: esa ruta ya representa «Mi perfil» y sus consultas están restringidas por `usuarios.medico_id`. La escritura continúa reservada a los roles autorizados.

La integración se deja como cambios locales revisables, sin publicar en el remoto. No se restablecen contraseñas ni se modifican cuentas de trabajo.

Validación posterior a la integración: **58 pruebas aprobadas en H2 y 55 en MariaDB**, sin fallos ni errores; sintaxis JavaScript verificada con `node --check` y revisión de conflictos con `git diff --check`.

### Corrección de edición de horarios

Se reprodujo el mensaje «Bad Request» al cambiar duración y vigencia a un rango sin el día semanal elegido. La causa de que no apareciera la explicación era `server.error.include-message`: en Spring Boot 4 esa propiedad fue reemplazada por `spring.web.error.include-message`.

El formulario valida fechas pasadas, rango final e inclusión del día semanal antes de enviar, y resalta el campo que debe corregirse. La fecha final tiene como mínimo la nueva fecha inicial. Los horarios cerrados hoy también se muestran como históricos; el backend impide reabrirlos al editar, preservando la versión que ya terminó.

Las pruebas HTTP cubren edición de duración y vigencia válidas, conservación histórica, rechazo de rangos sin atención, corrección posterior del rango y bloqueo de horarios cerrados. `node scripts/verificar-horarios-frontend.cjs` comprueba la validación real del formulario.

### Cambio de ejecución a la base real

La conexión actual, el inventario verificado, la revisión de inicializadores, la única cuenta nueva autorizada y los datos heredados pendientes están documentados en `CONEXION_LOCAL.md`. Se arrancó con `validate`, sin migraciones, importaciones de H2 ni pruebas de escritura sobre la base real.
