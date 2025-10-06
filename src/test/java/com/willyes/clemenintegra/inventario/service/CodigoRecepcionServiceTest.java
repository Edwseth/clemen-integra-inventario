package com.willyes.clemenintegra.inventario.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(CodigoRecepcionServiceImpl.class)
@TestPropertySource(properties = "spring.jpa.defer-datasource-initialization=true")
class CodigoRecepcionServiceTest {

    @Autowired
    private CodigoRecepcionService codigoRecepcionService;

    @Test
    void generaCodigosUnicosEnConcurrencia() throws InterruptedException, ExecutionException {
        LocalDate fecha = LocalDate.of(2025, 5, 20);
        int solicitudes = 10;
        ExecutorService executor = Executors.newFixedThreadPool(solicitudes);
        try {
            List<Callable<String>> tareas = IntStream.range(0, solicitudes)
                    .mapToObj(i -> (Callable<String>) () -> codigoRecepcionService.generarCodigo(fecha))
                    .toList();
            List<Future<String>> futures = executor.invokeAll(tareas);
            List<String> codigos = futures.stream().map(f -> {
                try {
                    return f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            assertThat(codigos).hasSize(solicitudes);
            assertThat(Set.copyOf(codigos)).hasSize(solicitudes);
            List<Integer> secuencias = codigos.stream()
                    .map(c -> c.substring(c.lastIndexOf('-') + 1))
                    .map(Integer::valueOf)
                    .sorted()
                    .collect(Collectors.toList());
            assertThat(secuencias).containsExactlyElementsOf(IntStream.rangeClosed(1, solicitudes)
                    .boxed()
                    .collect(Collectors.toList()));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void reiniciaSecuenciaPorDia() {
        LocalDate dia1 = LocalDate.of(2025, 5, 21);
        LocalDate dia2 = dia1.plusDays(1);

        String primeroDia1 = codigoRecepcionService.generarCodigo(dia1);
        String segundoDia1 = codigoRecepcionService.generarCodigo(dia1);
        String primeroDia2 = codigoRecepcionService.generarCodigo(dia2);

        assertThat(primeroDia1).isNotEqualTo(segundoDia1);
        assertThat(primeroDia1).startsWith("RC-" + dia1.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
        assertThat(primeroDia2).startsWith("RC-" + dia2.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));

        int secuenciaDia1 = Integer.parseInt(segundoDia1.substring(segundoDia1.lastIndexOf('-') + 1));
        int secuenciaDia2 = Integer.parseInt(primeroDia2.substring(primeroDia2.lastIndexOf('-') + 1));
        assertThat(secuenciaDia1).isGreaterThan(secuenciaDia2);
        assertThat(secuenciaDia2).isEqualTo(1);
    }
}
