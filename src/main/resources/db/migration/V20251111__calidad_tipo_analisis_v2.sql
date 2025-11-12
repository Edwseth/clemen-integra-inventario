-- Pre-chequeos
SELECT 'PRE_PRODUCTOS' AS etapa, tipo_analisis_calidad, COUNT(*) AS total FROM productos GROUP BY tipo_analisis_calidad;
SELECT 'PRE_EVALUACIONES' AS etapa, tipo_evaluacion, COUNT(*) AS total FROM evaluaciones_calidad GROUP BY tipo_evaluacion;
SELECT 'PRE_USUARIOS' AS etapa, rol, COUNT(*) AS total FROM usuarios GROUP BY rol;

-- Extiende temporalmente los ENUM para admitir los nuevos valores durante la migración
ALTER TABLE productos
    MODIFY COLUMN tipo_analisis_calidad ENUM('NINGUNO','FISICO_QUIMICO','MICROBIOLOGICO','AMBOS','FISICO','QUIMICO_MICROBIOLOGICO') NOT NULL;
ALTER TABLE evaluaciones_calidad
    MODIFY COLUMN tipo_evaluacion ENUM('FISICO_QUIMICO','MICROBIOLOGICO','FISICO','QUIMICO_MICROBIOLOGICO') NOT NULL;

-- Migración de datos a los nuevos literales
UPDATE productos
SET tipo_analisis_calidad = 'QUIMICO_MICROBIOLOGICO'
WHERE tipo_analisis_calidad = 'MICROBIOLOGICO';

UPDATE productos
SET tipo_analisis_calidad = 'AMBOS'
WHERE tipo_analisis_calidad = 'FISICO_QUIMICO';

UPDATE evaluaciones_calidad
SET tipo_evaluacion = 'QUIMICO_MICROBIOLOGICO'
WHERE tipo_evaluacion = 'MICROBIOLOGICO';

UPDATE evaluaciones_calidad
SET tipo_evaluacion = 'FISICO'
WHERE tipo_evaluacion = 'FISICO_QUIMICO';

-- Ajustes definitivos de ENUM
ALTER TABLE productos
    MODIFY COLUMN tipo_analisis_calidad ENUM('NINGUNO','FISICO','QUIMICO_MICROBIOLOGICO','AMBOS') NOT NULL;
ALTER TABLE evaluaciones_calidad
    MODIFY COLUMN tipo_evaluacion ENUM('FISICO','QUIMICO_MICROBIOLOGICO') NOT NULL;
ALTER TABLE usuarios
    MODIFY COLUMN rol ENUM('ROL_MICROBIOLOGO','ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_PRODUCCION','ROL_LIDER_HOMEOPATICOS','ROL_LIDER_ALIMENTOS','ROL_CONTADOR','ROL_COMPRADOR','ROL_SUPER_ADMIN') NOT NULL;

-- Post-chequeos
SELECT 'POST_PRODUCTOS' AS etapa, tipo_analisis_calidad, COUNT(*) AS total FROM productos GROUP BY tipo_analisis_calidad;
SELECT 'POST_EVALUACIONES' AS etapa, tipo_evaluacion, COUNT(*) AS total FROM evaluaciones_calidad GROUP BY tipo_evaluacion;
SELECT 'POST_USUARIOS' AS etapa, rol, COUNT(*) AS total FROM usuarios GROUP BY rol;

-- Rollback guidance (comentado)
-- UPDATE productos SET tipo_analisis_calidad = 'MICROBIOLOGICO' WHERE tipo_analisis_calidad = 'QUIMICO_MICROBIOLOGICO';
-- UPDATE productos SET tipo_analisis_calidad = 'FISICO_QUIMICO' WHERE tipo_analisis_calidad = 'AMBOS' AND <criterio_negocio>;
-- UPDATE evaluaciones_calidad SET tipo_evaluacion = 'MICROBIOLOGICO' WHERE tipo_evaluacion = 'QUIMICO_MICROBIOLOGICO';
-- UPDATE evaluaciones_calidad SET tipo_evaluacion = 'FISICO_QUIMICO' WHERE tipo_evaluacion = 'FISICO';
-- ALTER TABLE productos MODIFY COLUMN tipo_analisis_calidad ENUM('NINGUNO','FISICO_QUIMICO','MICROBIOLOGICO','AMBOS') NOT NULL;
-- ALTER TABLE evaluaciones_calidad MODIFY COLUMN tipo_evaluacion ENUM('FISICO_QUIMICO','MICROBIOLOGICO') NOT NULL;
-- ALTER TABLE usuarios MODIFY COLUMN rol ENUM('ROL_MICROBIOLOGO','ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_PRODUCCION','ROL_LIDER_HOMEOPATICOS','ROL_LIDER_ALIMENTOS','ROL_CONTADOR','ROL_COMPRADOR','ROL_SUPER_ADMIN') NOT NULL;
