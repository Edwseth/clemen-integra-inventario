package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicroDTO;
import com.willyes.clemenintegra.calidad.model.ParametroAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.enums.TipoResultadoAnalisis;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlantillaAnalisisMicroServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private PlantillaAnalisisMicroServiceImpl service;

    @Test
    void debeRequerirAnalisisCuandoProductoEsMicroYHayPlantillaConParametros() {
        PlantillaAnalisisMicrobiologico plantilla = crearPlantilla();
        Producto producto = Producto.builder()
                .id(1)
                .requiereAnalisisMicrobiologico(true)
                .plantillaAnalisisMicrobiologico(plantilla)
                .build();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        PlantillaAnalisisMicroDTO dto = service.obtenerPorProducto(1L);

        assertThat(dto.isRequiereAnalisisMicro()).isTrue();
        assertThat(dto.getParametros()).hasSize(1);
    }

    @Test
    void noDebeRequerirAnalisisCuandoTipoEsFisicoAunqueExistaPlantilla() {
        PlantillaAnalisisMicrobiologico plantilla = crearPlantilla();
        Producto producto = Producto.builder()
                .id(2)
                .requiereAnalisisFisico(true)
                .requiereAnalisisMicrobiologico(false)
                .plantillaAnalisisMicrobiologico(plantilla)
                .build();
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));

        PlantillaAnalisisMicroDTO dto = service.obtenerPorProducto(2L);

        assertThat(dto.isRequiereAnalisisMicro()).isFalse();
        assertThat(dto.getId()).isNull();
    }

    @Test
    void debeRequerirAnalisisCuandoTipoEsAmbosYHayPlantilla() {
        PlantillaAnalisisMicrobiologico plantilla = crearPlantilla();
        Producto producto = Producto.builder()
                .id(3)
                .requiereAnalisisQuimico(true)
                .requiereAnalisisMicrobiologico(true)
                .plantillaAnalisisMicrobiologico(plantilla)
                .build();
        when(productoRepository.findById(3L)).thenReturn(Optional.of(producto));

        PlantillaAnalisisMicroDTO dto = service.obtenerPorProducto(3L);

        assertThat(dto.isRequiereAnalisisMicro()).isTrue();
        assertThat(dto.getId()).isEqualTo(plantilla.getId());
    }

    @Test
    void noDebeRequerirAnalisisCuandoNoHayPlantilla() {
        Producto producto = Producto.builder()
                .id(4)
                .requiereAnalisisMicrobiologico(true)
                .build();
        when(productoRepository.findById(4L)).thenReturn(Optional.of(producto));

        PlantillaAnalisisMicroDTO dto = service.obtenerPorProducto(4L);

        assertThat(dto.isRequiereAnalisisMicro()).isFalse();
        assertThat(dto.getParametros()).isNull();
    }

    @Test
    void noDebeRequerirAnalisisCuandoPlantillaNoTieneParametros() {
        PlantillaAnalisisMicrobiologico plantilla = PlantillaAnalisisMicrobiologico.builder()
                .id(5L)
                .nombre("Plantilla sin params")
                .build();
        Producto producto = Producto.builder()
                .id(5)
                .requiereAnalisisQuimico(true)
                .requiereAnalisisMicrobiologico(true)
                .plantillaAnalisisMicrobiologico(plantilla)
                .build();
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));

        PlantillaAnalisisMicroDTO dto = service.obtenerPorProducto(5L);

        assertThat(dto.isRequiereAnalisisMicro()).isFalse();
    }

    private PlantillaAnalisisMicrobiologico crearPlantilla() {
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .id(10L)
                .nombreEnsayo("Mesofilos")
                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                .orden(1)
                .build();
        PlantillaAnalisisMicrobiologico plantilla = PlantillaAnalisisMicrobiologico.builder()
                .id(20L)
                .nombre("Plantilla micro")
                .build();
        plantilla.setParametros(List.of(parametro));
        return plantilla;
    }
}

