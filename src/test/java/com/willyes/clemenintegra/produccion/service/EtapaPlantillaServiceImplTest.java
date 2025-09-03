package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.produccion.dto.EtapaPlantillaReordenRequest;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtapaPlantillaServiceImplTest {

    @Mock EtapaPlantillaRepository repository;
    EtapaPlantillaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EtapaPlantillaServiceImpl(repository);
    }

    @Test
    void crearGuarda() {
        Producto prod = new Producto(); prod.setId(1);
        EtapaPlantilla etapa = EtapaPlantilla.builder()
                .nombre("E1").secuencia(1).activo(true).producto(prod).build();
        when(repository.existsByProductoIdAndSecuencia(1,1)).thenReturn(false);
        when(repository.existsByProductoIdAndNombreIgnoreCase(1,"E1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EtapaPlantilla res = service.crear(etapa);
        assertEquals("E1", res.getNombre());
        verify(repository).save(etapa);
    }

    @Test
    void crearSecuenciaDuplicada() {
        Producto prod = new Producto(); prod.setId(1);
        EtapaPlantilla etapa = EtapaPlantilla.builder()
                .nombre("E1").secuencia(1).producto(prod).build();
        when(repository.existsByProductoIdAndSecuencia(1,1)).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.crear(etapa));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void actualizarModifica() {
        Producto prod = new Producto(); prod.setId(1);
        EtapaPlantilla existente = EtapaPlantilla.builder()
                .id(5L).nombre("E1").secuencia(1).activo(true).producto(prod).build();
        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        when(repository.existsByProductoIdAndSecuenciaAndIdNot(1,2,5L)).thenReturn(false);
        when(repository.existsByProductoIdAndNombreIgnoreCaseAndIdNot(1,"E2",5L)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EtapaPlantilla cambios = EtapaPlantilla.builder().nombre("E2").secuencia(2).activo(false).build();
        EtapaPlantilla res = service.actualizar(5L, cambios);
        assertEquals("E2", res.getNombre());
        assertEquals(2, res.getSecuencia());
        assertFalse(res.getActivo());
        verify(repository).save(existente);
    }

    @Test
    void reordenarActualizaSoloCambios() {
        EtapaPlantilla e1 = EtapaPlantilla.builder().id(1L).secuencia(1).build();
        EtapaPlantilla e2 = EtapaPlantilla.builder().id(2L).secuencia(2).build();
        EtapaPlantilla e3 = EtapaPlantilla.builder().id(3L).secuencia(3).build();
        when(repository.findByProductoIdOrderBySecuenciaAsc(1)).thenReturn(List.of(e1,e2,e3));

        EtapaPlantillaReordenRequest r1 = new EtapaPlantillaReordenRequest(); r1.id=1L; r1.secuencia=2;
        EtapaPlantillaReordenRequest r2 = new EtapaPlantillaReordenRequest(); r2.id=2L; r2.secuencia=1;
        service.reordenar(1, List.of(r1,r2));

        ArgumentCaptor<List<EtapaPlantilla>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<EtapaPlantilla> guardadas = captor.getValue();
        assertEquals(2, guardadas.size());
        assertEquals(2, e1.getSecuencia());
        assertEquals(1, e2.getSecuencia());
    }
}
