package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.EspecificacionFisicoQuimicaRequest;
import com.willyes.clemenintegra.calidad.dto.ParametroAnalisisMicrobiologicoDTO;
import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicrobiologicoRequest;
import com.willyes.clemenintegra.calidad.mapper.EspecificacionFisicoQuimicaMapper;
import com.willyes.clemenintegra.calidad.mapper.PlantillaAnalisisMicrobiologicoMapper;
import com.willyes.clemenintegra.calidad.model.ParametroAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.enums.TipoResultadoAnalisis;
import com.willyes.clemenintegra.calidad.repository.EspecificacionFisicoQuimicaRepository;
import com.willyes.clemenintegra.calidad.repository.PlantillaAnalisisMicrobiologicoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EspecificacionesCalidadServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private EspecificacionFisicoQuimicaRepository especificacionFisicoQuimicaRepository;

    @Mock
    private PlantillaAnalisisMicrobiologicoRepository plantillaRepository;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private EspecificacionesCalidadServiceImpl service;

    @BeforeEach
    void setUp() {
        EspecificacionFisicoQuimicaMapper especificacionMapper = Mappers.getMapper(EspecificacionFisicoQuimicaMapper.class);
        PlantillaAnalisisMicrobiologicoMapper plantillaMapper = Mappers.getMapper(PlantillaAnalisisMicrobiologicoMapper.class);
        service = new EspecificacionesCalidadServiceImpl(
                productoRepository,
                especificacionFisicoQuimicaRepository,
                plantillaRepository,
                especificacionMapper,
                plantillaMapper,
                usuarioService
        );
    }

    @Test
    void debeCrearEspecificacionesFisicoQuimicasParaProductoFisico() {
        Producto producto = Producto.builder()
                .id(1)
                .codigoSku("SKU-01")
                .nombre("Producto FQ")
                .build();
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.FISICO);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        Usuario usuario = Usuario.builder().id(99L).nombreCompleto("Analista").build();
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        EspecificacionFisicoQuimicaRequest request = EspecificacionFisicoQuimicaRequest.builder()
                .nombreParametro("pH")
                .unidad("pH")
                .build();

        when(especificacionFisicoQuimicaRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.crearFisicoQuimicas(1L, List.of(request));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombreParametro()).isEqualTo("pH");
        assertThat(resultado.get(0).getCodigoSku()).isEqualTo("SKU-01");
    }

    @Test
    void debeRechazarEspecificacionesFisicoQuimicasParaProductoMicrobiologico() {
        Producto producto = Producto.builder()
                .id(2)
                .build();
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO);
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));

        EspecificacionFisicoQuimicaRequest request = EspecificacionFisicoQuimicaRequest.builder()
                .nombreParametro("pH")
                .build();

        assertThatThrownBy(() -> service.crearFisicoQuimicas(2L, List.of(request)))
                .isInstanceOf(CustomBusinessException.class);
    }

    @Test
    void debeCrearPlantillaMicrobiologicaVigenteV1() {
        Producto producto = Producto.builder()
                .id(3)
                .codigoSku("SKU-MICRO")
                .nombre("Producto Micro")
                .build();
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO);
        when(productoRepository.findById(3L)).thenReturn(Optional.of(producto));
        when(plantillaRepository.findTopByProducto_IdOrderByVersionDesc(3L)).thenReturn(Optional.empty());

        Usuario usuario = Usuario.builder().id(100L).nombreCompleto("Micro").build();
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        AtomicReference<PlantillaAnalisisMicrobiologico> saved = new AtomicReference<>();
        when(plantillaRepository.save(any())).thenAnswer(invocation -> {
            PlantillaAnalisisMicrobiologico plantilla = invocation.getArgument(0);
            plantilla.setId(200L);
            saved.set(plantilla);
            return plantilla;
        });
        when(plantillaRepository.findByProducto_IdOrderByVersionDesc(3L)).thenAnswer(invocation -> List.of(saved.get()));

        PlantillaAnalisisMicrobiologicoRequest request = PlantillaAnalisisMicrobiologicoRequest.builder()
                .nombre("Plantilla micro v1")
                .parametros(List.of(ParametroAnalisisMicrobiologicoDTO.builder()
                        .nombreParametro("Mesofilos")
                        .criterioAceptacion("<100")
                        .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                        .orden(1)
                        .build()))
                .build();

        var detalle = service.crearPlantillaMicro(3L, request);

        assertThat(detalle.getNumeroVersion()).isEqualTo(1);
        assertThat(detalle.isVigente()).isTrue();
        verify(productoRepository).save(producto);
    }

    @Test
    void debeClonarPlantillaYDejarUnaSolaVigente() {
        Producto producto = Producto.builder()
                .id(4)
                .codigoSku("SKU-M2")
                .nombre("Producto Micro 2")
                .build();
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO);

        PlantillaAnalisisMicrobiologico base = PlantillaAnalisisMicrobiologico.builder()
                .id(300L)
                .producto(producto)
                .nombre("Base")
                .version(1)
                .vigente(true)
                .parametros(List.of(ParametroAnalisisMicrobiologico.builder()
                        .nombreEnsayo("Coliformes")
                        .tipoResultado(TipoResultadoAnalisis.PRESENCIA_AUSENCIA)
                        .orden(1)
                        .build()))
                .build();

        when(plantillaRepository.findById(300L)).thenReturn(Optional.of(base));
        when(plantillaRepository.findTopByProducto_IdOrderByVersionDesc(4L)).thenReturn(Optional.of(base));
        when(plantillaRepository.findByProducto_IdOrderByVersionDesc(4L)).thenReturn(List.of(base));

        Usuario usuario = Usuario.builder().id(101L).nombreCompleto("Micro 2").build();
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        when(plantillaRepository.save(any())).thenAnswer(invocation -> {
            PlantillaAnalisisMicrobiologico plantilla = invocation.getArgument(0);
            plantilla.setId(301L);
            return plantilla;
        });

        service.clonarPlantillaMicroComoNuevaVersion(300L);

        assertThat(base.isVigente()).isFalse();
        ArgumentCaptor<List<PlantillaAnalisisMicrobiologico>> captor = ArgumentCaptor.forClass(List.class);
        verify(plantillaRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        verify(productoRepository).save(producto);
    }
}
