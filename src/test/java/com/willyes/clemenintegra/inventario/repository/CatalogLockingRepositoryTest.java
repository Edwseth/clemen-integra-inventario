package com.willyes.clemenintegra.inventario.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogLockingRepositoryTest {

    @Test
    void almacenRepositoryDoesNotDeclarePessimisticLocks() {
        for (Method method : AlmacenRepository.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(Lock.class))
                    .as("AlmacenRepository no debe declarar @Lock en %s", method.getName())
                    .isNull();
        }
    }

    @Test
    void solicitudMovimientoLockQueryDoesNotJoinCatalogs() throws NoSuchMethodException {
        Method method = SolicitudMovimientoRepository.class.getMethod("findByIdWithLock", Long.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value().toLowerCase())
                .doesNotContain("join fetch")
                .doesNotContain("almacen");
    }
}
