# Documentación: Fase 1 - Relaciones Hibernate/JPA (Meliton)

## Responsable
**Meliton** - Entidad: `HorarioAtencion` - Archivo: `Model/HorarioAtencion.java`

## Objetivo
Verificar y completar las relaciones `@ManyToOne` para `medico` y `consultorio` en la entidad `HorarioAtencion`, asegurando que las relaciones JPA/Hibernate sean correctas, eficientes y compatibles con la API REST del proyecto.

---

## Paso 1: Análisis del estado inicial

### 1.1 Lectura del archivo objetivo
Se leyó el archivo `src/main/java/com/tecsup/medicos_especialidades04/Model/HorarioAtencion.java`.

### 1.2 Estado inicial de las relaciones
El archivo ya contenía las dos relaciones `@ManyToOne` básicas:

```java
@ManyToOne
@JoinColumn(name = "medico_id", nullable = false)
private Medico medico;

@ManyToOne
@JoinColumn(name = "consultorio_id")
private Consultorio consultorio;
```

### 1.3 Verificación de la entidad relacionada `Medico`
Se leyó `src/main/java/com/tecsup/medicos_especialidades04/Model/Medico.java` (responsable: Angie).

- **Conclusión**: La entidad `Medico` aún no tiene el `@OneToMany` inverso hacia `HorarioAtencion` (esa es la tarea de Angie). Por lo tanto, desde el punto de vista de `HorarioAtencion`, la relación `@ManyToOne` hacia `Medico` es correcta y no requiere cambios en `Medico`.

### 1.4 Verificación de la entidad relacionada `Consultorio`
Se leyó `src/main/java/com/tecsup/medicos_especialidades04/Model/Consultorio.java` (responsable: Luis).

- **Conclusión**: La entidad `Consultorio` ya tiene la relación inversa `@OneToMany(mappedBy = "consultorio")` correctamente configurada:

```java
@OneToMany(mappedBy = "consultorio")
@com.fasterxml.jackson.annotation.JsonIgnore
private List<HorarioAtencion> horarios = new ArrayList<>();
```

- El `mappedBy = "consultorio"` hace referencia al nombre del campo `consultorio` en `HorarioAtencion`, lo cual **coincide** con el nombre del atributo en nuestra entidad. La relación bidireccional está correctamente mapeada.

### 1.5 Verificación de la guía de trabajo en equipo
Se leyó `GUIA_TRABAJO_EQUIPO.md`. La tarea asignada a Meliton es:

> **Meliton | Horarios | `Model/HorarioAtencion.java` | Verificar `@ManyToOne` medico/consultorio**

---

## Paso 2: Identificación de mejoras necesarias

### 2.1 Problema: `FetchType.EAGER` por defecto en `@ManyToOne`
Por defecto, las relaciones `@ManyToOne` en JPA usan `FetchType.EAGER`, lo que significa que **siempre** se carga la entidad relacionada (Medico o Consultorio) al momento de cargar un `HorarioAtencion`. Esto causa:

- **Problema de rendimiento (N+1)**: Si se listan 100 horarios, se ejecutan 100 consultas adicionales para cargar cada Medico y cada Consultorio.
- **Ineficiencia innecesaria**: No siempre se necesita acceder a los datos del médico o consultorio al listar horarios.

**Solución**: Agregar `fetch = FetchType.LAZY` a ambas relaciones `@ManyToOne`.

### 2.2 Problema: Serialización JSON con proxies de Hibernate
Cuando se usa `FetchType.LAZY`, Hibernate crea **proxies** (clases proxy) para las entidades relacionadas. Al serializar a JSON (la API REST devuelve objetos como JSON), Jackson intenta acceder a campos internos del proxy (`hibernateLazyInitializer`, `handler`), lo que puede causar:

- Errores de serialización (`StackOverflowError` o `JsonMappingException`).
- Exposición innecesaria de metadatos internos de Hibernate.

**Solución**: Agregar `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` a ambas relaciones.

### 2.3 Problema: Comentarios TODO obsoletos
El archivo contenía comentarios `TODO (Meliton)` que deben ser reemplazados por comentarios que reflejen el trabajo completado.

---

## Paso 3: Aplicación de los cambios

### 3.1 Agregar import de `JsonIgnoreProperties`
Se agregó la importación necesaria:

```java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
```

### 3.2 Modificar la relación `@ManyToOne medico`
**Antes:**
```java
@ManyToOne
@JoinColumn(name = "medico_id", nullable = false)
private Medico medico;
```

**Después:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "medico_id", nullable = false)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
private Medico medico;
```

**Explicación de cada anotación:**
- `@ManyToOne(fetch = FetchType.LAZY)`: Relación muchos-a-uno. Un horario pertenece a un médico. `LAZY` evita cargar el médico hasta que se acceda explícitamente.
- `@JoinColumn(name = "medico_id", nullable = false)`: La columna de unión se llama `medico_id` en la tabla `horario_atencion`. `nullable = false` garantiza que **todo horario tenga un médico asociado** (no puede ser nulo).
- `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})`: Evita errores de serialización JSON al ignorar los campos internos del proxy de Hibernate.

### 3.3 Modificar la relación `@ManyToOne consultorio`
**Antes:**
```java
@ManyToOne
@JoinColumn(name = "consultorio_id")
private Consultorio consultorio;
```

**Después:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "consultorio_id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
private Consultorio consultorio;
```

**Explicación de cada anotación:**
- `@ManyToOne(fetch = FetchType.LAZY)`: Relación muchos-a-uno. Un horario puede estar asociado a un consultorio. `LAZY` para rendimiento.
- `@JoinColumn(name = "consultorio_id")`: La columna de unión se llama `consultorio_id`. **No** se especifica `nullable = false`, lo que permite que un horario exista **sin** un consultorio asignado (caso válido: un médico puede tener horarios sin consultorio específico).
- `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})`: Mismo tratamiento que para medico.

### 3.4 Actualizar comentarios
Se reemplazaron los comentarios `TODO` por comentarios descriptivos:

```java
// Pregunta 1 - Relaciones: (Meliton) relaciones @ManyToOne verificadas y completadas
```

Y dentro de la clase:

```java
// Relaciones @ManyToOne verificadas: medico (obligatorio) y consultorio (opcional)
```

---

## Paso 4: Verificación de la compilación

### 4.1 Comando ejecutado
```bash
mvn compile -q
```

### 4.2 Resultado
- **Exit code**: 0
- **Resultado**: `BUILD SUCCESS`
- **Conclusión**: El proyecto compila correctamente con los cambios aplicados. No se introdujeron errores de compilación.

---

## Paso 5: Resumen de cambios realizados

| Archivo | Cambio | Responsable |
|---|---|---|
| `Model/HorarioAtencion.java` | Agregado `fetch = FetchType.LAZY` a `@ManyToOne medico` | Meliton |
| `Model/HorarioAtencion.java` | Agregado `fetch = FetchType.LAZY` a `@ManyToOne consultorio` | Meliton |
| `Model/HorarioAtencion.java` | Agregado `@JsonIgnoreProperties` a ambas relaciones | Meliton |
| `Model/HorarioAtencion.java` | Agregado import de `JsonIgnoreProperties` | Meliton |
| `Model/HorarioAtencion.java` | Reemplazado comentarios `TODO` por comentarios descriptivos | Meliton |

### Archivos NO modificados (respetando la restricción)
- `Model/Medico.java` (Angie) - No se tocó.
- `Model/Consultorio.java` (Luis) - No se tocó.
- `Model/Especialidades.java` (Mayra) - No se tocó.
- Cualquier otro archivo del proyecto - No se tocó.

---

## Paso 6: Conceptos clave aprendidos

### 6.1 `@ManyToOne`
- Relación donde **muchos** objetos `HorarioAtencion` pueden referenciar a **uno** `Medico` o **uno** `Consultorio`.
- Es la **propietaria** de la relación (contiene la clave foránea).
- Por defecto usa `FetchType.EAGER` (se recomienda cambiar a `LAZY`).

### 6.2 `@JoinColumn`
- Especifica la columna que actúa como clave foránea.
- `name`: nombre de la columna en la tabla.
- `nullable`: si `false`, la columna no puede ser nula (obligatoria).

### 6.3 `FetchType.LAZY`
- La entidad relacionada se carga **solo cuando se accede** a ella (perezosa).
- Mejora el rendimiento al evitar consultas innecesarias.
- Requiere que la sesión de Hibernate esté abierta al acceder (o usar `@JsonIgnoreProperties` para evitar problemas en JSON).

### 6.4 `@JsonIgnoreProperties`
- Anotación de Jackson que indica qué propiedades **ignorar** durante la serialización/deserialización JSON.
- `{"hibernateLazyInitializer", "handler"}` son campos internos que Hibernate agrega a los proxies. Ignorarlos evita errores de serialización.

### 6.5 Relación bidireccional
- `HorarioAtencion` → `Medico` (lado propietario, `@ManyToOne`)
- `Consultorio` → `HorarioAtencion` (lado inverso, `@OneToMany(mappedBy = "consultorio")`)
- El `mappedBy` debe coincidir con el nombre del **atributo** en el lado propietario.

---

## Paso 7: Consideraciones para pruebas (CRUD)

Según la guía, se deben probar las relaciones con CRUD. Para `HorarioAtencion`:

1. **Crear (POST)**: `POST /api/horarios` con un body JSON que incluya `medico_id` y opcionalmente `consultorio_id`.
2. **Listar (GET)**: `GET /api/horarios` - con `LAZY`, los campos `medico` y `consultorio` se serializan correctamente gracias a `@JsonIgnoreProperties`.
3. **Obtener por ID (GET)**: `GET /api/horarios/{id}` - accede al médico y consultorio asociados.
4. **Actualizar (PUT)**: `PUT /api/horarios/{id}` - actualiza el médico y/o consultorio.
5. **Eliminar (DELETE)**: `DELETE /api/horarios/{id}` - elimina el horario (no afecta al médico o consultorio).

### Verificación de la base de datos
Con `spring.jpa.hibernate.ddl-auto=update` en `application.properties`, Hibernate generará automáticamente las columnas `medico_id` y `consultorio_id` en la tabla `horario_atencion` como claves foráneas.

---

## Conclusión

La tarea de Meliton se ha completado. Las relaciones `@ManyToOne` para `medico` y `consultorio` en `HorarioAtencion.java` han sido verificadas y mejoradas con:

- `FetchType.LAZY` para optimizar el rendimiento.
- `@JsonIgnoreProperties` para evitar errores de serialización JSON.
- Comentarios actualizados que reflejan el trabajo completado.

El proyecto compila correctamente (`BUILD SUCCESS`). No se modificaron archivos de otros compañeros.
