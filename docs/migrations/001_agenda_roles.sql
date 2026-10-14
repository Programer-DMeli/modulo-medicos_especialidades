-- Migracion manual MySQL 8, una sola vez sobre el esquema anterior a esta version.
-- Respalde la base y ejecute primero las consultas de comprobacion del README.
-- No ejecutar sobre una base nueva creada con docs/schema.sql.

ALTER TABLE roles ADD COLUMN estado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE horario_atencion
    ADD COLUMN vigente_desde DATE NULL,
    ADD COLUMN vigente_hasta DATE NULL;

CREATE TABLE ausencia_medico (
    id_ausencia BIGINT NOT NULL AUTO_INCREMENT,
    medico_id BIGINT NOT NULL,
    inicio DATETIME(6) NOT NULL,
    fin DATETIME(6) NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_ausencia),
    CONSTRAINT fk_ausencia_medico FOREIGN KEY (medico_id) REFERENCES medico (id_medico),
    INDEX ix_ausencia_medico_intervalo (medico_id, inicio, fin)
) ENGINE=InnoDB;

-- No se inventan fechas de vigencia para registros anteriores.
-- Revisarlos en la interfaz y establecer fechas conocidas antes de reutilizarlos.
