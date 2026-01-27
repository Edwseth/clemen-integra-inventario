package com.willyes.clemenintegra.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

// Activa TestcontainersExtension para los tests que necesitan Docker (vía @Testcontainers).
@Testcontainers
@DockerTest
@TestPropertySource(properties = {
        "spring.flyway.locations=classpath:db/migration/inventario",
        "spring.jpa.hibernate.ddl-auto=none"
})
public abstract class IntegrationTestMySqlContainer {

    // El contenedor se inicializa de forma perezosa para evitar fallos sin Docker.
    private static MySQLContainer<?> mysql;

    @BeforeAll
    static void setUpMySqlContainer() {
        Assumptions.assumeTrue(isDockerAvailable(),
                "Docker no disponible; se omiten tests con Testcontainers");
        startContainerIfNeeded();
    }

    @DynamicPropertySource
    static void registerMySqlProperties(DynamicPropertyRegistry registry) {
        if (!isDockerAvailable()) {
            Assumptions.assumeTrue(false,
                    "Docker no disponible; se omiten tests con Testcontainers");
            return;
        }
        startContainerIfNeeded();
        if (mysql == null) {
            return;
        }
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @AfterAll
    static void tearDownMySqlContainer() {
        if (mysql != null) {
            mysql.stop();
        }
    }

    private static synchronized void startContainerIfNeeded() {
        if (mysql != null) {
            return;
        }
        mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("clemen_test")
                .withUsername("test")
                .withPassword("test");
        mysql.start();
    }

    private static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
