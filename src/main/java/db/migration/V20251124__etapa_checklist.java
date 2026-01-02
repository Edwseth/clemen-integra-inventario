package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V20251124__etapa_checklist extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var conn = context.getConnection();
        try (var st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS etapa_checklist_item (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        etapa_produccion_id BIGINT NOT NULL,
                        nombre_paso VARCHAR(255) NOT NULL,
                        obligatorio BOOLEAN NOT NULL DEFAULT FALSE,
                        completado BOOLEAN NOT NULL DEFAULT FALSE,
                        observacion TEXT NULL,
                        completed_at DATETIME NULL,
                        completed_by_id BIGINT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        created_by_id BIGINT NULL,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        updated_by_id BIGINT NULL,
                        CONSTRAINT fk_checklist_etapa FOREIGN KEY (etapa_produccion_id)
                            REFERENCES etapa_produccion(id)
                            ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            try {
                st.execute("""
                        CREATE INDEX idx_checklist_etapa_id
                        ON etapa_checklist_item (etapa_produccion_id);
                        """);
            } catch (Exception ignored) {
                // índice puede existir en entornos previos
            }
        }
    }
}
