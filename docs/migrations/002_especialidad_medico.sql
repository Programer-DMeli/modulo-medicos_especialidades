-- Migracion manual MySQL 8 posterior a 001_agenda_roles.sql.
-- Ejecute las comprobaciones de duplicados antes del ALTER. Corregir los datos
-- repetidos antes de instalar los indices unicos. Respaldar y ejecutar una vez.

-- Debe devolver cero filas. CMP duplicado impide instalar uk_medico_cmp.
SELECT cmp, COUNT(*) AS cantidad FROM medico GROUP BY cmp HAVING COUNT(*) > 1;

-- Debe devolver cero filas. Pares repetidos impiden instalar uk_medico_especialidad.
SELECT id_medico, id_especialidad, COUNT(*) AS cantidad
FROM medico_especialidad
GROUP BY id_medico, id_especialidad
HAVING COUNT(*) > 1;

ALTER TABLE medico
    ADD CONSTRAINT uk_medico_cmp UNIQUE (cmp);

ALTER TABLE medico_especialidad
    ADD CONSTRAINT uk_medico_especialidad UNIQUE (id_medico, id_especialidad);

ALTER TABLE horario_atencion
    ADD COLUMN especialidad_id BIGINT NULL,
    ADD CONSTRAINT fk_horario_especialidad
        FOREIGN KEY (especialidad_id) REFERENCES especialidad (id_especialidad);

-- No se completa especialidad_id automáticamente: no se puede inferir de forma
-- segura en médicos con más de una especialidad. Los datos NULL requieren revisión.
