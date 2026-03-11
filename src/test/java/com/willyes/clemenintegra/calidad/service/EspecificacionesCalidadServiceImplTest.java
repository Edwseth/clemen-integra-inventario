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
import java.util.stream.IntStream;

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
                        .metodo("ISO 4833-1:2013 / USP <61>")
                        .criterioAceptacion("<100")
                        .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                        .orden(1)
                        .build()))
                .build();

        var detalle = service.crearPlantillaMicro(3L, request);

        assertThat(detalle.getNumeroVersion()).isEqualTo(1);
        assertThat(detalle.isVigente()).isTrue();
        assertThat(saved.get().getParametros()).hasSize(1);
        assertThat(saved.get().getParametros().get(0).getMetodo()).isEqualTo("ISO 4833-1:2013 / USP <61>");
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


    @Test
    void debeClonarPlantillaParaOtroProductoConVersionUnoYVigente() {
        Producto productoOrigen = Producto.builder().id(7).build();
        productoOrigen.setTipoAnalisisCalidad(TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO);
        Producto productoDestino = Producto.builder().id(8).codigoSku("SKU-D").nombre("Destino").build();
        productoDestino.setTipoAnalisisCalidad(TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO);

        ParametroAnalisisMicrobiologico parametroOrigen = ParametroAnalisisMicrobiologico.builder()
                .id(1L)
                .nombreEnsayo("Coliformes")
                .metodo("ISO")
                .unidad("UFC/g")
                .especificacion("<10")
                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                .orden(1)
                .build();

        PlantillaAnalisisMicrobiologico plantillaOrigen = PlantillaAnalisisMicrobiologico.builder()
                .id(800L)
                .producto(productoOrigen)
                .nombre("Plantilla origen")
                .descripcion("Desc")
                .version(4)
                .vigente(true)
                .parametros(List.of(parametroOrigen))
                .build();

        when(plantillaRepository.findById(800L)).thenReturn(Optional.of(plantillaOrigen));
        when(productoRepository.findById(8L)).thenReturn(Optional.of(productoDestino));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(Usuario.builder().id(10L).build());
        when(plantillaRepository.save(any())).thenAnswer(invocation -> {
            PlantillaAnalisisMicrobiologico plantilla = invocation.getArgument(0);
            plantilla.setId(801L);
            return plantilla;
        });

        var detalle = service.clonarPlantillaMicroParaProducto(800L, 8L);

        assertThat(detalle.getId()).isEqualTo(801L);
        assertThat(detalle.getNumeroVersion()).isEqualTo(1);
        assertThat(detalle.isVigente()).isTrue();
        assertThat(detalle.getParametros()).hasSize(1);
        assertThat(detalle.getParametros().get(0).getNombreParametro()).isEqualTo("Coliformes");

        ArgumentCaptor<PlantillaAnalisisMicrobiologico> captor = ArgumentCaptor.forClass(PlantillaAnalisisMicrobiologico.class);
        verify(plantillaRepository).save(captor.capture());
        PlantillaAnalisisMicrobiologico guardada = captor.getValue();
        assertThat(guardada.getProducto()).isSameAs(productoDestino);
        assertThat(guardada.getParametros()).hasSize(1);
        assertThat(guardada.getParametros().get(0)).isNotSameAs(parametroOrigen);
        assertThat(plantillaOrigen.getVersion()).isEqualTo(4);
        assertThat(plantillaOrigen.isVigente()).isTrue();
        verify(plantillaRepository, never()).saveAll(any());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void debeListarPlantillaLegacyCuandoNoHayPlantillasPorProducto() {
        Producto producto = Producto.builder()
                .id(5)
                .codigoSku("MP0126")
                .nombre("CHONTADURO")
                .build();

        PlantillaAnalisisMicrobiologico legacy = PlantillaAnalisisMicrobiologico.builder()
                .id(500L)
                .producto(producto)
                .nombre("Plantilla legacy")
                .version(1)
                .vigente(true)
                .build();
        producto.setPlantillaAnalisisMicrobiologico(legacy);

        when(plantillaRepository.findByProducto_IdOrderByVersionDesc(5L)).thenReturn(List.of());
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));

        var resultado = service.listarPlantillasMicroPorProducto(5L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(500L);
        assertThat(resultado.get(0).getCodigoSku()).isEqualTo("MP0126");
    }

    @Test
    void debeMapearNumeroVersionVigenteYParametrosEnResumenMicro() {
        PlantillaAnalisisMicrobiologico plantilla = PlantillaAnalisisMicrobiologico.builder()
                .id(600L)
                .nombre("Micro MP")
                .version(2)
                .vigente(true)
                .parametros(IntStream.rangeClosed(1, 10)
                        .mapToObj(i -> ParametroAnalisisMicrobiologico.builder()
                                .id((long) i)
                                .nombreEnsayo("Parametro " + i)
                                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                                .orden(i)
                                .build())
                        .toList())
                .build();

        when(plantillaRepository.findByProducto_IdOrderByVersionDesc(6L)).thenReturn(List.of(plantilla));

        var resultado = service.listarPlantillasMicroPorProducto(6L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNumeroVersion()).isEqualTo(2);
        assertThat(resultado.get(0).isVigente()).isTrue();
        assertThat(resultado.get(0).getNumeroParametros()).isEqualTo(10);
    }

    @Test
    void debeIncluirMetodoEnDetalleDeParametrosMicro() {
        ParametroAnalisisMicrobiologico parametro = ParametroAnalisisMicrobiologico.builder()
                .id(701L)
                .nombreEnsayo("Mesofilos")
                .metodo("ISO 4833-1:2013 / USP <61>")
                .unidad("UFC/g")
                .especificacion("<100")
                .tipoResultado(TipoResultadoAnalisis.NUMERICO)
                .orden(1)
                .build();
        PlantillaAnalisisMicrobiologico plantilla = PlantillaAnalisisMicrobiologico.builder()
                .id(700L)
                .nombre("Plantilla micro detalle")
                .parametros(List.of(parametro))
                .build();

        when(plantillaRepository.findById(700L)).thenReturn(Optional.of(plantilla));

        var detalle = service.obtenerDetallePlantillaMicro(700L);

        assertThat(detalle.getParametros()).hasSize(1);
        assertThat(detalle.getParametros().get(0).getNombreParametro()).isEqualTo("Mesofilos");
        assertThat(detalle.getParametros().get(0).getMetodo()).isEqualTo("ISO 4833-1:2013 / USP <61>");
        assertThat(detalle.getParametros().get(0).getCriterioAceptacion()).isEqualTo("<100");
        assertThat(detalle.getParametros().get(0).getTipoResultado()).isEqualTo(TipoResultadoAnalisis.NUMERICO);
    }
}
