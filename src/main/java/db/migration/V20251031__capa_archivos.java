package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V20251031__capa_archivos extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var conn = context.getConnection();
        try (var st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS capa_archivos (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        capa_id BIGINT NOT NULL,
                        nombre_archivo VARCHAR(255) NOT NULL,
                        nombre_visible VARCHAR(255) NULL,
                        content_type VARCHAR(150) NULL,
                        tamano_bytes BIGINT NULL,
                        creado_por BIGINT NULL,
                        fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_capa_archivos_capa FOREIGN KEY (capa_id)
                            REFERENCES capa (id)
                            ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            try {
                st.execute("""
                        CREATE INDEX idx_capa_archivos_capa_id
                        ON capa_archivos (capa_id);
                        """);
            } catch (Exception ignored) {
                // Si ya existe el índice, continuar
            }
        }
    }
}
