CREATE TABLE IF NOT EXISTS archivos_evaluacion (
    evaluacion_id BIGINT NOT NULL,
    archivo VARCHAR(255) NOT NULL,
    FOREIGN KEY (evaluacion_id) REFERENCES evaluaciones_calidad(id)
);

ALTER TABLE evaluaciones_calidad DROP COLUMN IF EXISTS archivo_adjunto;