-- Aplicar una vez. La columna nullable conserva las cuentas ya existentes.
ALTER TABLE usuarios
    ADD COLUMN medico_id BIGINT NULL,
    ADD CONSTRAINT uk_usuario_medico UNIQUE (medico_id),
    ADD CONSTRAINT fk_usuario_medico FOREIGN KEY (medico_id) REFERENCES medico (id_medico);
