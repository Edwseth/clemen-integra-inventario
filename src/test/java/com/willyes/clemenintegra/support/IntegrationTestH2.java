package com.willyes.clemenintegra.support;

import org.springframework.test.context.TestPropertySource;

/**
 * Base para pruebas de integración sin Docker: usa el datasource H2 definido en application-test.properties.
 */
@TestPropertySource(properties = {
        // Usa migraciones compatibles con H2 para estas pruebas.
        "spring.flyway.locations=classpath:db/migration/test",
        // Completa columnas faltantes sin tocar migraciones productivas.
        "spring.jpa.hibernate.ddl-auto=update"
})
public abstract class IntegrationTestH2 {
    // Sin anotaciones de Testcontainers para evitar el TestcontainersExtension.
}
