ALTER TABLE usuarios
    ADD COLUMN nivel_acceso_admin VARCHAR(20) NOT NULL DEFAULT 'FULL';
