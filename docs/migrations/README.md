# Configuracion y migracion

La aplicacion usa MySQL por defecto, con `ddl-auto=validate`. Configure `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` en el entorno. No hay credenciales reales en los perfiles. El codigo anterior contenia una clave en el repositorio: si sigue siendo valida, cambiela en su servidor; retirar el texto del archivo no revoca esa clave.

## Base nueva

Cree una base de evaluacion independiente ejecutando `docs/schema.sql`. Cambie su nombre en el script y en `DB_URL` si necesita conservar una base existente. Los roles base se insertan al arrancar; no se crea una cuenta administradora en MySQL. Configure una cuenta inicial mediante el procedimiento autorizado de su entorno, con password BCrypt.

## Base existente

1. Haga un respaldo y pruebe primero en una copia. Seleccione explicitamente la base correcta en su cliente MySQL.
2. Compruebe el estado del esquema:

```sql
SELECT DATABASE();
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND ((TABLE_NAME = 'roles' AND COLUMN_NAME = 'estado')
    OR (TABLE_NAME = 'horario_atencion' AND COLUMN_NAME IN ('vigente_desde','vigente_hasta'))
    OR TABLE_NAME = 'ausencia_medico');
SHOW CREATE TABLE medico;
SHOW CREATE TABLE horario_atencion;
```

3. Si aun no existen los campos/tabla de agenda, ejecute `001_agenda_roles.sql` una vez. Si una parte ya existe, aplique solo las sentencias que falten tras comparar su definicion. MySQL confirma operaciones DDL inmediatamente; el script no puede deshacerse con un rollback.
4. Ejecute las dos consultas de deteccion de duplicados en `002_especialidad_medico.sql`. Corrija cualquier resultado; despues aplique la migracion una sola vez. Añade CMP unico, evita asignar dos veces una especialidad al mismo médico y agrega especialidad opcional a cada horario.
5. Compruebe `roles.estado`, las vigencias y las claves foraneas. Los horarios anteriores conservan fechas desconocidas y especialidad en NULL: revise y configure la informacion real en la interfaz. No se asignan valores inventados.
6. Arranque usando las variables de entorno. Una incompatibilidad de esquema debe resolverse en la copia antes de aplicar la version a la base original.

## Demostracion aislada

Ejecute `mvn spring-boot:run -Dspring-boot.run.profiles=demo` para seleccionar explicitamente H2 en memoria. Este perfil contiene solo datos ficticios y se pierde al cerrar. No utilice variables `SPRING_DATASOURCE_*` ni sobrescriba la conexion del perfil demo.

| Usuario ficticio | Rol | Password demo |
|---|---|---|
| admin.demo | ADMINISTRADOR | Demo2026! |
| recepcion.demo | RECEPCIONISTA | Demo2026! |
| medico.demo | MEDICO | Demo2026! |

El perfil demo crea plantillas semanales con vigencias relativas a la semana de ejecucion, consultorios y una ausencia. No se activa por defecto ni crea cuentas en MySQL.

## Pruebas

`mvn test` utiliza el perfil `test` en cada prueba de integracion. H2 es independiente de MySQL y sus tablas se recrean para el contexto de pruebas. Se conservan las pruebas JPA originales y se verifican autorizacion, CSRF, login, estados de usuario/rol y auditoria con principal autenticado. Esto no sustituye la evidencia de ejecucion final en una copia MySQL requerida para la sustentacion.
