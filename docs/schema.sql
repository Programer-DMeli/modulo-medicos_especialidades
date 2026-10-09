-- ============================================================================
-- Base de datos: medico_especialidad
-- Script de estructura (MySQL 8) generado a partir de las entidades JPA
-- del proyecto medicos_especialidades04.
--
-- Nota: la aplicacion tambien crea/actualiza las tablas automaticamente con
-- spring.jpa.hibernate.ddl-auto=update; este script queda como referencia
-- y para restaurar la BD desde cero.
-- Los 3 roles por defecto (ADMINISTRADOR, MEDICO, RECEPCIONISTA) los crea
-- DataInitializer al arrancar la aplicacion.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS medico_especialidad
    DEFAULT CHARACTER SET utf8mb4;
USE medico_especialidad;

-- ----------------------------------------------------------------------------
-- roles
-- ----------------------------------------------------------------------------
CREATE TABLE roles (
    id_rol      BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(50)  NOT NULL,
    descripcion VARCHAR(200) NULL,
    PRIMARY KEY (id_rol),
    CONSTRAINT uk_roles_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- usuarios
-- ----------------------------------------------------------------------------
CREATE TABLE usuarios (
    id_usuario     BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL,
    password       VARCHAR(100) NOT NULL, -- hash BCrypt
    nombres        VARCHAR(100) NOT NULL,
    correo         VARCHAR(150) NULL,
    estado         BOOLEAN      NOT NULL, -- true = activo, false = desactivado
    id_rol         BIGINT       NULL,
    fecha_creacion DATETIME(6)  NULL,
    PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuarios_username UNIQUE (username),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (id_rol) REFERENCES roles (id_rol)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- medico
-- ----------------------------------------------------------------------------
CREATE TABLE medico (
    id_medico        BIGINT       NOT NULL AUTO_INCREMENT,
    codigo_medico    VARCHAR(20)  NOT NULL,
    tipo_documento   VARCHAR(10)  NOT NULL,
    numero_documento VARCHAR(15)  NOT NULL,
    nombres          VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(100) NOT NULL,
    apellido_materno VARCHAR(100) NOT NULL,
    cmp              VARCHAR(20)  NOT NULL,
    rne              VARCHAR(20)  NULL,
    telefono         VARCHAR(15)  NULL,
    correo           VARCHAR(150) NULL,
    fecha_ingreso    DATE         NOT NULL,
    estado           VARCHAR(20)  NOT NULL, -- ACTIVO | SUSPENDIDO | VACACIONES
    PRIMARY KEY (id_medico),
    CONSTRAINT uk_medico_codigo    UNIQUE (codigo_medico),
    CONSTRAINT uk_medico_documento UNIQUE (numero_documento)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- especialidad
-- ----------------------------------------------------------------------------
CREATE TABLE especialidad (
    id_especialidad   BIGINT       NOT NULL AUTO_INCREMENT,
    codigo            VARCHAR(20)  NOT NULL,
    nombre            VARCHAR(100) NOT NULL,
    descripcion       VARCHAR(200) NULL,
    duracion_consulta INT          NOT NULL,
    estado            BOOLEAN      NOT NULL,
    PRIMARY KEY (id_especialidad),
    CONSTRAINT uk_especialidad_codigo UNIQUE (codigo),
    CONSTRAINT uk_especialidad_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- consultorios
-- ----------------------------------------------------------------------------
CREATE TABLE consultorios (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    codigo          VARCHAR(20)  NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    piso            INT          NOT NULL,
    area            VARCHAR(100) NOT NULL,
    estado          VARCHAR(20)  NOT NULL, -- DISPONIBLE | OCUPADO | INACTIVO
    especialidad_id BIGINT       NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_consultorios_codigo UNIQUE (codigo),
    CONSTRAINT fk_consultorios_especialidad
        FOREIGN KEY (especialidad_id) REFERENCES especialidad (id_especialidad)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- horario_atencion
-- ----------------------------------------------------------------------------
CREATE TABLE horario_atencion (
    id_horario     BIGINT      NOT NULL AUTO_INCREMENT,
    medico_id      BIGINT      NOT NULL,
    consultorio_id BIGINT      NULL, -- consultorio opcional
    dia_semana     VARCHAR(20) NOT NULL,
    hora_inicio    TIME        NOT NULL,
    hora_fin       TIME        NOT NULL,
    duracion_cita  INT         NOT NULL,
    estado         VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_horario),
    CONSTRAINT fk_horario_medico      FOREIGN KEY (medico_id)      REFERENCES medico (id_medico),
    CONSTRAINT fk_horario_consultorio FOREIGN KEY (consultorio_id) REFERENCES consultorios (id)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- medico_especialidad (relacion muchos a muchos)
-- ----------------------------------------------------------------------------
CREATE TABLE medico_especialidad (
    id_medico_especialidad BIGINT       NOT NULL AUTO_INCREMENT,
    id_medico              BIGINT       NOT NULL,
    id_especialidad        BIGINT       NOT NULL,
    subespecialidad        VARCHAR(100) NULL,
    PRIMARY KEY (id_medico_especialidad),
    CONSTRAINT fk_me_medico      FOREIGN KEY (id_medico)      REFERENCES medico (id_medico),
    CONSTRAINT fk_me_especialidad FOREIGN KEY (id_especialidad) REFERENCES especialidad (id_especialidad)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- auditoria (registro automatico de INSERT / UPDATE / DELETE)
-- ----------------------------------------------------------------------------
CREATE TABLE auditoria (
    id_auditoria BIGINT       NOT NULL AUTO_INCREMENT,
    usuario      VARCHAR(100) NULL,
    fecha_hora   DATETIME(6)  NULL,
    operacion    VARCHAR(20)  NULL, -- INSERT | UPDATE | DELETE
    entidad      VARCHAR(50)  NULL,
    registro_id  BIGINT       NULL,
    PRIMARY KEY (id_auditoria)
) ENGINE = InnoDB;
