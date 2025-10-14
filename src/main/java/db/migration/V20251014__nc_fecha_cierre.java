package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V20251014__nc_fecha_cierre extends BaseJavaMigration {
    private static final Logger LOGGER = LoggerFactory.getLogger(V20251014__nc_fecha_cierre.class);

    @Override
    public void migrate(Context context) throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));

        String existsQuery = """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'no_conformidad'
                  AND COLUMN_NAME = 'fecha_cierre'
                """;

        Integer count = jdbcTemplate.queryForObject(existsQuery, Integer.class);
        int existingColumns = count != null ? count : 0;

        if (existingColumns == 0) {
            LOGGER.info("[MIGRATION] Añadiendo columna fecha_cierre en no_conformidad");
            jdbcTemplate.execute("ALTER TABLE no_conformidad ADD COLUMN fecha_cierre DATETIME NULL");
        } else {
            LOGGER.info("[MIGRATION] La columna fecha_cierre ya existe en no_conformidad. No se realizan cambios");
        }
    }
}