package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V20251020__calidad_micro_plantillas extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var conn = context.getConnection();
        try (var st = conn.createStatement()) {
            if (isH2(context)) {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS plantillas_analisis_micro (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            nombre VARCHAR(255) NOT NULL,
                            descripcion TEXT NULL,
                            activo BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP NULL,
                            created_by_id BIGINT NULL
                        );
                        """);

                st.execute("""
                        CREATE TABLE IF NOT EXISTS parametros_analisis_micro (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            plantilla_id BIGINT NOT NULL,
                            nombre_ensayo VARCHAR(255) NOT NULL,
                            unidad VARCHAR(100) NULL,
                            especificacion VARCHAR(255) NULL,
                            tipo_resultado VARCHAR(30) NOT NULL,
                            orden_parametro INT NULL,
                            CONSTRAINT fk_parametro_plantilla FOREIGN KEY (plantilla_id)
                                REFERENCES plantillas_analisis_micro (id)
                                ON DELETE CASCADE
                        );
                        """);

                st.execute("""
                        CREATE TABLE IF NOT EXISTS resultados_analisis_micro (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            evaluacion_id BIGINT NOT NULL,
                            parametro_id BIGINT NOT NULL,
                            resultado TEXT NULL,
                            cumple BOOLEAN NULL,
                            observaciones TEXT NULL,
                            CONSTRAINT fk_res_micro_eval FOREIGN KEY (evaluacion_id)
                                REFERENCES evaluaciones_calidad (id)
                                ON DELETE CASCADE,
                            CONSTRAINT fk_res_micro_param FOREIGN KEY (parametro_id)
                                REFERENCES parametros_analisis_micro (id)
                                ON DELETE CASCADE
                        );
                        """);
            } else {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS plantillas_analisis_micro (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            nombre VARCHAR(255) NOT NULL,
                            descripcion TEXT NULL,
                            activo TINYINT(1) NOT NULL DEFAULT 1,
                            created_at DATETIME NULL,
                            created_by_id BIGINT NULL
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                        """);

                st.execute("""
                        CREATE TABLE IF NOT EXISTS parametros_analisis_micro (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            plantilla_id BIGINT NOT NULL,
                            nombre_ensayo VARCHAR(255) NOT NULL,
                            unidad VARCHAR(100) NULL,
                            especificacion VARCHAR(255) NULL,
                            tipo_resultado ENUM('NUMERICO','PRESENCIA_AUSENCIA','TEXTO') NOT NULL,
                            orden_parametro INT NULL,
                            CONSTRAINT fk_parametro_plantilla FOREIGN KEY (plantilla_id)
                                REFERENCES plantillas_analisis_micro (id)
                                ON DELETE CASCADE
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                        """);

                st.execute("""
                        CREATE TABLE IF NOT EXISTS resultados_analisis_micro (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            evaluacion_id BIGINT NOT NULL,
                            parametro_id BIGINT NOT NULL,
                            resultado TEXT NULL,
                            cumple TINYINT(1) NULL,
                            observaciones TEXT NULL,
                            CONSTRAINT fk_res_micro_eval FOREIGN KEY (evaluacion_id)
                                REFERENCES evaluaciones_calidad (id)
                                ON DELETE CASCADE,
                            CONSTRAINT fk_res_micro_param FOREIGN KEY (parametro_id)
                                REFERENCES parametros_analisis_micro (id)
                                ON DELETE CASCADE
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                        """);
            }

            st.execute("""
                    ALTER TABLE productos
                    ADD COLUMN IF NOT EXISTS plantilla_micro_id BIGINT NULL;
                    """);

            try {
                st.execute("""
                        ALTER TABLE productos
                        ADD CONSTRAINT fk_productos_plantilla_micro FOREIGN KEY (plantilla_micro_id)
                            REFERENCES plantillas_analisis_micro (id);
                        """);
            } catch (Exception ignored) {
                // Si ya existe la FK, continuar
            }
        }
    }

    private boolean isH2(Context context) throws Exception {
        return context.getConnection()
                .getMetaData()
                .getDatabaseProductName()
                .toLowerCase()
                .contains("h2");
    }
}
