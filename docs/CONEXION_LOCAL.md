# Ejecución local contra XAMPP/MariaDB

Verificación de lectura: 21 de octubre de 2026. Rama `feature/test_integration`.

- Frontend: http://127.0.0.1:8081/login
- Perfil activo: `local`. Es también el perfil predeterminado de Spring.
- Servidor: MariaDB 10.4.32 en `127.0.0.1:3306`.
- Base: `medico_especialidad`.
- JDBC: `jdbc:mysql://127.0.0.1:3306/medico_especialidad`.
- SQLyog es el cliente de administración; la aplicación conecta directamente al servidor.
- Hibernate: `spring.jpa.hibernate.ddl-auto=validate`. El arranque validó el esquema existente.

## Arranque reproducible

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
mvn.cmd -o package -DskipTests
.\scripts\iniciar-local.ps1
```

El script usa el puerto 8081 y mantiene el proceso en primer plano. Puede indicar otro puerto con `-Port`. No lo ejecute si la instancia actual ya ocupa ese puerto. El proceso actual se identifica en `target/panel-local.pid`; sus registros están en `target/panel-local.log` y `target/panel-local-error.log`.

`DB_USERNAME` y `DB_PASSWORD` permiten reutilizar las credenciales del servidor sin escribir secretos en el repositorio. La configuración normal usa la conexión de XAMPP ya existente. El perfil `demo` sigue siendo una opción explícita de desarrollo y no está activo; la instancia demo anterior fue detenida por su PID y su línea de ejecución, sin detener MariaDB ni otros servicios.

## Datos comprobados

| Tabla real | API | Filas |
|---|---|---:|
| medico | /api/medicos | 4 |
| especialidad | /api/especialidades | 3 |
| medico_especialidad | /api/medico/especialidades | 1 |
| horario_atencion | /api/horarios | 3 |
| consultorios | /api/consultorios | 3 |
| ausencia_medico | /api/horarios/ausencias | 0 |
| auditoria | /api/auditoria | 26 |
| roles | /api/roles | 3 |
| usuarios | /api/usuarios | 8 |

Cada listado respondió HTTP 200 y sus IDs coincidieron exactamente con consultas SQL de lectura a esta base. Se verificó en el navegador la carga de las tablas, el inicio, la agenda y el estado vacío de ausencias. Los nombres «horarios» y «Horario_Atencion» corresponden al módulo y su tabla física `horario_atencion`; no se creó otra tabla.

No se ejecutaron pruebas de escritura ni se crearon bases de integración. No se importaron datos desde H2. El empaquetado se realizó con las pruebas omitidas para no dirigir ninguna suite a la base real.

## Inicializadores y preservación

- `DemoDataInitializer`: requiere el perfil `demo`; no se ejecutó.
- `DataInitializer`: crea únicamente roles ausentes; los tres roles base ya estaban presentes y sus filas permanecieron sin cambios.
- `CuentasMedicosInitializer`: conserva la creación automática de cuentas MEDICO faltantes. Los cuatro médicos ya tenían cuenta; no generó cuentas ni modificó credenciales o estados.
- Se comprobó mediante huellas por fila que las siete cuentas existentes conservaron todos sus campos, incluidas contraseñas y estados. Las sumas de comprobación de médicos, especialidades, asignaciones, horarios, consultorios, ausencias y roles coincidieron antes y después de las verificaciones.

Por autorización expresa posterior del usuario, se creó únicamente la nueva cuenta administrativa `panel` y su registro de auditoría. Eso explica el paso de 7 a 8 usuarios y de 25 a 26 eventos. Su acceso está en `.local/acceso-panel.json`, protegido por permisos del usuario de Windows y SYSTEM y excluido de Git. La contraseña no se imprime en respuestas ni registros. Los usuarios nuevos mantienen nombres cortos y las nuevas contraseñas automáticas tienen 12 caracteres aleatorios; las cuentas existentes no se renombran ni se restablecen.

## Permisos y pendientes

Se conservan las reglas existentes de administrador y recepción. MEDICO continúa limitado a `usuarios.medico_id`, con perfil, especialidades y agenda propios de consulta; una cuenta sin vínculo recibe el aviso y el bloqueo existentes. La verificación real utilizó solo la nueva cuenta administrativa, sin usar cuentas `.demo` ni cambiar las credenciales de otros roles.

No se detectaron tablas o columnas faltantes y no fue necesaria ninguna migración. Dos horarios heredados carecen de vigencia y de especialidad: cargan en el listado como pendientes de completar y no generan atención en la agenda. Actualmente la agenda de hoy no tiene bloques; no se inventaron fechas ni asignaciones para rellenarla. Estos datos deberán completarse mediante una operación autorizada de administración o recepción.

Los resultados de lectura están en `target/verificacion-base-real.txt` y `target/verificacion-integridad-real.txt`.
