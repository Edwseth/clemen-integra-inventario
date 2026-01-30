package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.LoteConsecutivoDia;
import com.willyes.clemenintegra.produccion.repository.LoteConsecutivoDiaRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(LoteConsecutivoDiaService.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.flyway.enabled=false",
        "DB_SECURPASS=dummy",
        "DB_SECURNAME=dummy"
})
class LoteConsecutivoDiaServiceTest {

    @Autowired
    private LoteConsecutivoDiaService service;

    @Autowired
    private LoteConsecutivoDiaRepository repository;

    @Test
    @DisplayName("Debe iniciar en 000 y continuar en 001")
    void debeIniciarEnCeroYContinuarEnUno() {
        LocalDate fecha = LocalDate.of(2025, 2, 1);

        int primero = service.obtenerSiguienteConsecutivo(fecha);
        int segundo = service.obtenerSiguienteConsecutivo(fecha);

        assertThat(primero).isZero();
        assertThat(segundo).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe fallar cuando el consecutivo supera 999")
    void debeFallarEnOverflow() {
        LocalDate fecha = LocalDate.of(2025, 2, 2);
        repository.save(LoteConsecutivoDia.builder()
                .fecha(fecha)
                .ultimoValor(999)
                .build());

        assertThatThrownBy(() -> service.obtenerSiguienteConsecutivo(fecha))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException response = (ResponseStatusException) ex;
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                })
                .hasMessageContaining("CONSECUTIVO_DIARIO_EXCEDIDO");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Debe asignar consecutivos distintos en llamadas concurrentes")
    @Disabled("H2 puede arrojar violaciones de PK durante inserciones simultáneas; concurrencia se valida en MySQL.")
    void debeAsignarConsecutivosDistintosEnConcurrente() throws Exception {
        LocalDate fecha = LocalDate.of(2025, 2, 3);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Integer> tarea = () -> {
            ready.countDown();
            start.await();
            return service.obtenerSiguienteConsecutivo(fecha);
        };

        Future<Integer> first = executor.submit(tarea);
        Future<Integer> second = executor.submit(tarea);

        ready.await();
        start.countDown();

        List<Integer> resultados = List.of(first.get(), second.get());
        Set<Integer> unicos = resultados.stream().collect(Collectors.toSet());

        assertThat(unicos).hasSize(2);
        assertThat(unicos).containsExactlyInAnyOrder(0, 1);

        executor.shutdown();
    }
}
