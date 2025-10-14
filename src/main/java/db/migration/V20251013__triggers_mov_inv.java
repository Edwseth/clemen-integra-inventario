package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V20251013__triggers_mov_inv extends BaseJavaMigration {
    private static final Logger LOGGER = LoggerFactory.getLogger(V20251013__triggers_mov_inv.class);

    @Override
    public void migrate(Context context) throws Exception {
        var conn = context.getConnection();
        try (var st = conn.createStatement()) {
            String db;
            try (var rs = st.executeQuery("SELECT DATABASE()")) {
                rs.next();
                db = rs.getString(1);
            }
            LOGGER.info("[MIGRATION] Creando triggers en schema: {}", db);

            st.execute("DROP TRIGGER IF EXISTS trg_mov_inv_chk_entrada_pt");
            st.execute("""
                        CREATE TRIGGER trg_mov_inv_chk_entrada_pt
                        BEFORE INSERT ON %s.movimientos_inventario
                        FOR EACH ROW
                        BEGIN
                          IF NEW.tipo_mov = 'ENTRADA'
                             AND NEW.motivos_movimiento_id = 6
                             AND NEW.orden_produccion_id IS NULL THEN
                            SIGNAL SQLSTATE '45000'
                              SET MESSAGE_TEXT = 'ENTRADA_PT requiere orden_produccion_id';
                          END IF;
                        END
                    """.formatted("`" + db + "`"));

            st.execute("DROP TRIGGER IF EXISTS trg_mov_inv_chk_entrada_pt_u");
            st.execute("""
                        CREATE TRIGGER trg_mov_inv_chk_entrada_pt_u
                        BEFORE UPDATE ON %s.movimientos_inventario
                        FOR EACH ROW
                        BEGIN
                          IF NEW.tipo_mov = 'ENTRADA'
                             AND NEW.motivos_movimiento_id = 6
                             AND NEW.orden_produccion_id IS NULL THEN
                            SIGNAL SQLSTATE '45000'
                              SET MESSAGE_TEXT = 'ENTRADA_PT requiere orden_produccion_id (UPDATE)';
                          END IF;
                        END
                    """.formatted("`" + db + "`"));
            LOGGER.info("[MIGRATION] Triggers creados/actualizados OK en {}", db);
        }
    }
}
