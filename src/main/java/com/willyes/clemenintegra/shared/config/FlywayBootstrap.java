package com.willyes.clemenintegra.shared.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

@Component
@Order(0) // Corre temprano
@ConditionalOnProperty(prefix = "app.flyway.bootstrap", name = "enabled", havingValue = "true")
public class FlywayBootstrap implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(FlywayBootstrap.class);

    @Value("${spring.datasource.url}")
    private String primaryUrl;
    @Value("${spring.datasource.username:${DB_USERNAME:root}}")
    private String primaryUser;
    @Value("${spring.datasource.password:${DB_PASS:}}")
    private String primaryPass;

    @Value("${app.flyway.secondary.url}")
    private String secondaryUrl;
    @Value("${app.flyway.secondary.user:${DB_USERNAME:root}}")
    private String secondaryUser;
    @Value("${app.flyway.secondary.password:${DB_PASS:}}")
    private String secondaryPass;

    @Value("${app.flyway.migrations.locations:classpath:db/migration}")
    private String locations;

    @Value("${app.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${app.flyway.baseline-version:1}")
    private String baselineVersion;

    @Override
    public void run(String... args) {
        migrateIfFirstTime(primaryUrl, primaryUser, primaryPass);
        migrateIfFirstTime(secondaryUrl, secondaryUser, secondaryPass);
    }

    private void migrateIfFirstTime(String url, String user, String pass) {
        try {
            boolean hasHistory = hasFlywayHistory(url, user, pass);
            Flyway flyway = Flyway.configure()
                    .dataSource(url, user, pass)
                    .locations(locations)
                    .baselineOnMigrate(baselineOnMigrate)
                    .baselineVersion(baselineVersion)
                    .outOfOrder(false)
                    .load();

            if (!hasHistory) {
                log.info("Flyway[once]: SIN historial en {} → baseline+migrate (baselineVersion={})",
                        url, baselineVersion);
            } else {
                log.info("Flyway[once]: historial YA presente en {} → se ejecuta migrate para aplicar pendientes.",
                        url);
            }

            flyway.migrate();
            var info = flyway.info();
            log.info("Flyway[once]: OK en {} → applied={}, current={}",
                    url, info.applied().length,
                    info.current() == null ? "n/a" : info.current().getVersion());
        } catch (Exception e) {
            log.error("Flyway[once]: ERROR migrando {}", url, e);
            throw e;
        }
    }

    private boolean hasFlywayHistory(String url, String user, String pass) {
        try (Connection cn = DriverManager.getConnection(url, user, pass)) {
            try (ResultSet rs = cn.getMetaData().getTables(null, null, "flyway_schema_history", null)) {
                if (!rs.next()) return false; // no existe la tabla
            }
            try (ResultSet rs2 = cn.createStatement().executeQuery("SELECT 1 FROM flyway_schema_history LIMIT 1")) {
                return rs2.next();
            }
        } catch (Exception e) {
            return false; // si falla, asumimos que NO hay historial
        }
    }
}

