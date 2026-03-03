package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisCreateRequest;
import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisDetalleDTO;
import com.willyes.clemenintegra.calidad.dto.PlantillaCampoRequest;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisis;
import com.willyes.clemenintegra.calidad.model.enums.TipoAnalisisPlantilla;
import com.willyes.clemenintegra.calidad.model.enums.TipoCampoPlantilla;
import com.willyes.clemenintegra.calidad.repository.PlantillaAnalisisRepository;
import com.willyes.clemenintegra.calidad.repository.PlantillaCampoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantillasAnalisisServiceImplTest {

    @Mock
    private PlantillaAnalisisRepository plantillaAnalisisRepository;
    @Mock
    private PlantillaCampoRepository plantillaCampoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private PlantillasAnalisisServiceImpl service;

    @BeforeEach
    void setup() {
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(Usuario.builder().id(9L).build());
    }

    @Test
    void crearPlantillaFisicoValida() {
        Producto producto = Producto.builder().id(1).tipoAnalisisCalidad(TipoAnalisisCalidad.FISICO).build();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(plantillaAnalisisRepository.findTopByProducto_IdAndTipoAnalisisOrderByVersionDesc(1L, TipoAnalisisPlantilla.FISICO))
                .thenReturn(Optional.empty());
        when(plantillaAnalisisRepository.save(any())).thenAnswer(invocation -> {
            PlantillaAnalisis p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        PlantillaAnalisisDetalleDTO dto = service.crearPlantilla(1L, TipoAnalisisPlantilla.FISICO,
                PlantillaAnalisisCreateRequest.builder()
                        .nombre("Checklist físico")
                        .campos(List.of(PlantillaCampoRequest.builder()
                                .codigo("apariencia")
                                .label("Apariencia")
                                .tipoCampo(TipoCampoPlantilla.TRIESTADO)
                                .orden(1)
                                .build()))
                        .build());

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getTipoAnalisis()).isEqualTo(TipoAnalisisPlantilla.FISICO);
        assertThat(dto.getCampos()).hasSize(1);
    }

    @Test
    void rechazaCrearPlantillaSiProductoNoPermiteFisico() {
        Producto producto = Producto.builder().id(1).tipoAnalisisCalidad(TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO).build();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.crearPlantilla(1L, TipoAnalisisPlantilla.FISICO,
                PlantillaAnalisisCreateRequest.builder().build()))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessageContaining("no permite");
    }

    @Test
    void clonarIncrementaVersionYNoMarcaVigente() {
        Producto producto = Producto.builder().id(1).tipoAnalisisCalidad(TipoAnalisisCalidad.AMBOS).build();
        PlantillaAnalisis base = PlantillaAnalisis.builder()
                .id(1L)
                .producto(producto)
                .tipoAnalisis(TipoAnalisisPlantilla.FISICO)
                .version(3)
                .vigente(true)
                .build();
        when(plantillaAnalisisRepository.findById(1L)).thenReturn(Optional.of(base));
        when(plantillaAnalisisRepository.findTopByProducto_IdAndTipoAnalisisOrderByVersionDesc(1L, TipoAnalisisPlantilla.FISICO))
                .thenReturn(Optional.of(base));
        when(plantillaAnalisisRepository.save(any())).thenAnswer(invocation -> {
            PlantillaAnalisis p = invocation.getArgument(0);
            p.setId(7L);
            return p;
        });

        PlantillaAnalisisDetalleDTO dto = service.clonarComoNuevaVersion(1L);

        assertThat(dto.getVersion()).isEqualTo(4);
        assertThat(dto.isVigente()).isFalse();
    }

    @Test
    void marcarVigenteDesactivaAnteriores() {
        Producto producto = Producto.builder().id(1).tipoAnalisisCalidad(TipoAnalisisCalidad.FISICO).build();
        PlantillaAnalisis vigenteAnterior = PlantillaAnalisis.builder()
                .id(1L).producto(producto).tipoAnalisis(TipoAnalisisPlantilla.FISICO).vigente(true).build();
        PlantillaAnalisis nueva = PlantillaAnalisis.builder()
                .id(2L).producto(producto).tipoAnalisis(TipoAnalisisPlantilla.FISICO).vigente(false).build();

        when(plantillaAnalisisRepository.findById(2L)).thenReturn(Optional.of(nueva));
        when(plantillaAnalisisRepository.findByProducto_IdAndTipoAnalisisAndVigenteTrue(1L, TipoAnalisisPlantilla.FISICO))
                .thenReturn(List.of(vigenteAnterior));
        when(plantillaAnalisisRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.marcarVigente(2L);

        ArgumentCaptor<List<PlantillaAnalisis>> captor = ArgumentCaptor.forClass(List.class);
        verify(plantillaAnalisisRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).isVigente()).isFalse();
    }

    @Test
    void crearCampoConCodigoDuplicadoLanzaError() {
        Producto producto = Producto.builder().id(1).tipoAnalisisCalidad(TipoAnalisisCalidad.FISICO).build();
        PlantillaAnalisis plantilla = PlantillaAnalisis.builder().id(2L).producto(producto).tipoAnalisis(TipoAnalisisPlantilla.FISICO).build();
        when(plantillaAnalisisRepository.findById(2L)).thenReturn(Optional.of(plantilla));
        when(plantillaCampoRepository.existsByPlantilla_IdAndCodigo(2L, "APARIENCIA")).thenReturn(true);

        assertThatThrownBy(() -> service.crearCampo(2L, PlantillaCampoRequest.builder()
                .codigo("apariencia")
                .label("Apariencia")
                .tipoCampo(TipoCampoPlantilla.TRIESTADO)
                .orden(1)
                .build())).isInstanceOf(CustomBusinessException.class);
    }
}
