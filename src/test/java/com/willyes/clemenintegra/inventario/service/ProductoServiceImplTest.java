package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.ProductoOptionDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.ProductoMapper;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductoServiceImplTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private UnidadMedidaRepository unidadMedidaRepository;
    @Mock private CategoriaProductoRepository categoriaProductoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LoteProductoRepository loteProductoRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock private ProductoMapper productoMapper;
    @Mock private StockQueryService stockQueryService;

    @InjectMocks
    private ProductoServiceImpl service;

    @BeforeEach
    void setUpSecurity() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(new Usuario(1L)));
        when(productoMapper.toDto(any(Producto.class))).thenReturn(new ProductoResponseDTO());
        when(stockQueryService.obtenerStockDisponible(any(Long.class))).thenReturn(BigDecimal.ZERO);
        when(stockQueryService.obtenerStockDisponible(anyList())).thenReturn(Collections.emptyMap());
        when(loteProductoRepository.existsByProducto(any(Producto.class))).thenReturn(false);
        when(movimientoInventarioRepository.existsByProductoId(any(Long.class))).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            producto.setId(10);
            return producto;
        });
    }

    @Test
    @DisplayName("Debe conservar rendimiento para PT y SKU válido")
    void crearProducto_conservaRendimientoParaPt() {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(5L);
        when(unidadMedidaRepository.findById(5L)).thenReturn(Optional.of(unidad));

        CategoriaProducto categoriaPt = new CategoriaProducto();
        categoriaPt.setId(3L);
        categoriaPt.setTipo(TipoCategoria.PRODUCTO_TERMINADO);
        when(categoriaProductoRepository.findById(3L)).thenReturn(Optional.of(categoriaPt));

        when(productoRepository.existsByCodigoSku("PT0001")).thenReturn(false);
        when(productoRepository.existsByNombre("Producto PT")).thenReturn(false);

        ProductoRequestDTO dto = ProductoRequestDTO.builder()
                .sku("PT0001")
                .nombre("Producto PT")
                .descripcionProducto("desc")
                .stockMinimo(BigDecimal.ONE)
                .unidadMedidaId(5L)
                .categoriaProductoId(3L)
                .rendimientoUnidad(new BigDecimal("100.234"))
                .build();

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);

        service.crearProducto(dto, 1L);

        verify(productoRepository).save(captor.capture());
        assertThat(captor.getValue().getRendimientoUnidad())
                .isEqualByComparingTo(new BigDecimal("100.23"));
    }

    @Test
    @DisplayName("Debe conservar rendimiento para PS con SKU válido")
    void crearProducto_conservaRendimientoParaPs() {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(6L);
        when(unidadMedidaRepository.findById(6L)).thenReturn(Optional.of(unidad));

        CategoriaProducto categoriaPs = new CategoriaProducto();
        categoriaPs.setId(4L);
        categoriaPs.setTipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        when(categoriaProductoRepository.findById(4L)).thenReturn(Optional.of(categoriaPs));

        when(productoRepository.existsByCodigoSku("PS0001")).thenReturn(false);
        when(productoRepository.existsByNombre("Producto PS")).thenReturn(false);

        ProductoRequestDTO dto = ProductoRequestDTO.builder()
                .sku("PS0001")
                .nombre("Producto PS")
                .descripcionProducto("desc")
                .stockMinimo(BigDecimal.ONE)
                .unidadMedidaId(6L)
                .categoriaProductoId(4L)
                .rendimientoUnidad(new BigDecimal("50"))
                .build();

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        service.crearProducto(dto, 1L);
        verify(productoRepository).save(captor.capture());

        assertThat(captor.getValue().getRendimientoUnidad())
                .isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Debe rechazar PS sin rendimiento")
    void crearProducto_psSinRendimiento_lanzaExcepcion() {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(7L);
        when(unidadMedidaRepository.findById(7L)).thenReturn(Optional.of(unidad));

        CategoriaProducto categoriaPs = new CategoriaProducto();
        categoriaPs.setId(8L);
        categoriaPs.setTipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        when(categoriaProductoRepository.findById(8L)).thenReturn(Optional.of(categoriaPs));

        when(productoRepository.existsByCodigoSku("PS0002")).thenReturn(false);
        when(productoRepository.existsByNombre("PS Sin Rend"))
                .thenReturn(false);

        ProductoRequestDTO dto = ProductoRequestDTO.builder()
                .sku("PS0002")
                .nombre("PS Sin Rend")
                .descripcionProducto("desc")
                .stockMinimo(BigDecimal.ONE)
                .unidadMedidaId(7L)
                .categoriaProductoId(8L)
                .rendimientoUnidad(null)
                .build();

        assertThatThrownBy(() -> service.crearProducto(dto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rendimiento por unidad es obligatorio");
    }

    @Test
    @DisplayName("Debe devolver solo productos fabricables (PT y PS)")
    void findProductosFabricables_devuelveFabricables() {
        CategoriaProducto categoriaPt = categoria(TipoCategoria.PRODUCTO_TERMINADO);
        CategoriaProducto categoriaPs = categoria(TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        CategoriaProducto categoriaMp = categoria(TipoCategoria.MATERIA_PRIMA);

        Producto pt = Producto.builder().id(1).codigoSku("PT-001").categoriaProducto(categoriaPt).build();
        Producto ps = Producto.builder().id(2).codigoSku("PS-001").categoriaProducto(categoriaPs).build();
        Producto mp = Producto.builder().id(3).codigoSku("MP-001").categoriaProducto(categoriaMp).build();

        when(productoRepository.findByCategoriaProducto_TipoIn(anyList()))
                .thenReturn(List.of(pt, ps, mp));
        when(stockQueryService.obtenerStockDisponible(List.of(1L, 2L)))
                .thenReturn(Map.of(1L, BigDecimal.ONE, 2L, new BigDecimal("2.50")));
        when(productoMapper.toDto(pt)).thenReturn(ProductoResponseDTO.builder().id(1L).sku("PT-001").build());
        when(productoMapper.toDto(ps)).thenReturn(ProductoResponseDTO.builder().id(2L).sku("PS-001").build());

        List<ProductoResponseDTO> resultado = service.findProductosFabricables();

        assertThat(resultado)
                .extracting(ProductoResponseDTO::getId)
                .containsExactly(1L, 2L);
        assertThat(resultado)
                .extracting(ProductoResponseDTO::getSku)
                .containsExactly("PT-001", "PS-001");
    }

    @ParameterizedTest(name = "Crear producto con banderas F={0} Q={1} M={2}")
    @MethodSource("banderasCalidad")
    void crearProducto_persisteBanderasCalidad(Boolean requiereFisico,
                                               Boolean requiereQuimico,
                                               Boolean requiereMicro) {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(5L);
        when(unidadMedidaRepository.findById(5L)).thenReturn(Optional.of(unidad));

        CategoriaProducto categoriaMp = new CategoriaProducto();
        categoriaMp.setId(3L);
        categoriaMp.setTipo(TipoCategoria.MATERIA_PRIMA);
        when(categoriaProductoRepository.findById(3L)).thenReturn(Optional.of(categoriaMp));

        when(productoRepository.existsByCodigoSku("MP0001")).thenReturn(false);
        when(productoRepository.existsByNombre("Producto MP")).thenReturn(false);

        ProductoRequestDTO dto = ProductoRequestDTO.builder()
                .sku("MP0001")
                .nombre("Producto MP")
                .descripcionProducto("desc")
                .stockMinimo(BigDecimal.ONE)
                .unidadMedidaId(5L)
                .categoriaProductoId(3L)
                .requiereAnalisisFisico(requiereFisico)
                .requiereAnalisisQuimico(requiereQuimico)
                .requiereAnalisisMicrobiologico(requiereMicro)
                .build();

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        service.crearProducto(dto, 1L);

        verify(productoRepository).save(captor.capture());
        Producto guardado = captor.getValue();
        assertThat(guardado.isRequiereAnalisisFisico()).isEqualTo(Boolean.TRUE.equals(requiereFisico));
        assertThat(guardado.isRequiereAnalisisQuimico()).isEqualTo(Boolean.TRUE.equals(requiereQuimico));
        assertThat(guardado.isRequiereAnalisisMicrobiologico()).isEqualTo(Boolean.TRUE.equals(requiereMicro));
    }

    @Test
    @DisplayName("Actualizar producto debe reemplazar banderas de calidad")
    void actualizarProducto_actualizaBanderasCalidad() {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(5L);
        when(unidadMedidaRepository.findById(5L)).thenReturn(Optional.of(unidad));

        CategoriaProducto categoriaMp = new CategoriaProducto();
        categoriaMp.setId(3L);
        categoriaMp.setTipo(TipoCategoria.MATERIA_PRIMA);
        when(categoriaProductoRepository.findById(3L)).thenReturn(Optional.of(categoriaMp));

        Producto existente = Producto.builder()
                .id(10)
                .codigoSku("MP0001")
                .nombre("Producto MP")
                .stockMinimo(BigDecimal.ONE)
                .unidadMedida(unidad)
                .categoriaProducto(categoriaMp)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .build();
        when(productoRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(productoRepository.existsByCodigoSkuAndIdNot("MP0001", 10L)).thenReturn(false);
        when(productoRepository.existsByNombreAndIdNot("Producto MP", 10L)).thenReturn(false);

        ProductoRequestDTO dto = ProductoRequestDTO.builder()
                .sku("MP0001")
                .nombre("Producto MP")
                .descripcionProducto("desc")
                .stockMinimo(BigDecimal.ONE)
                .unidadMedidaId(5L)
                .categoriaProductoId(3L)
                .requiereAnalisisFisico(true)
                .requiereAnalisisQuimico(true)
                .requiereAnalisisMicrobiologico(false)
                .build();

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        service.actualizarProducto(10L, dto, 1L);

        verify(productoRepository).save(captor.capture());
        Producto actualizado = captor.getValue();
        assertThat(actualizado.isRequiereAnalisisFisico()).isTrue();
        assertThat(actualizado.isRequiereAnalisisQuimico()).isTrue();
        assertThat(actualizado.isRequiereAnalisisMicrobiologico()).isFalse();
    }

    @Test
    @DisplayName("Debe devolver página vacía si el término de autocomplete está vacío")
    void buscarInsumosAutocomplete_sinTermino_devuelveVacio() {
        Pageable pageable = PageRequest.of(0, 5);

        Page<Producto> resultado = service.buscarInsumosAutocomplete("   ", pageable);

        assertThat(resultado).isEmpty();
        verify(productoRepository, never()).buscarInsumosAutocomplete(anyList(), anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Debe buscar insumos en categorías permitidas y mapear término recortado")
    void buscarInsumosAutocomplete_conTermino_invocaRepositorioConFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        Producto producto = new Producto();
        Page<Producto> page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.buscarInsumosAutocomplete(anyList(), anyString(), any(Pageable.class)))
                .thenReturn(page);

        Page<Producto> resultado = service.buscarInsumosAutocomplete("  mp ", pageable);

        assertThat(resultado.getContent()).containsExactly(producto);

        ArgumentCaptor<List<TipoCategoria>> tiposCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> termCaptor = ArgumentCaptor.forClass(String.class);
        verify(productoRepository).buscarInsumosAutocomplete(tiposCaptor.capture(), termCaptor.capture(), any(Pageable.class));

        assertThat(tiposCaptor.getValue())
                .containsExactlyInAnyOrder(
                        TipoCategoria.MATERIA_PRIMA,
                        TipoCategoria.MATERIAL_EMPAQUE,
                        TipoCategoria.SUMINISTROS,
                        TipoCategoria.PRODUCTO_SEMI_ELABORADO
                );
        assertThat(termCaptor.getValue()).isEqualTo("mp");
    }

    @Test
    @DisplayName("Debe devolver página vacía si el término de fabricables está vacío")
    void buscarProductosFabricablesAutocomplete_sinTermino_devuelveVacio() {
        Pageable pageable = PageRequest.of(0, 5);

        Page<Producto> resultado = service.buscarProductosFabricablesAutocomplete("   ", pageable);

        assertThat(resultado).isEmpty();
        verify(productoRepository, never()).buscarFabricablesAutocomplete(anyList(), anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Debe buscar fabricables en PT y PS y normalizar el término")
    void buscarProductosFabricablesAutocomplete_conTermino_invocaRepositorioConFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        Producto producto = new Producto();
        Page<Producto> page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.buscarFabricablesAutocomplete(anyList(), anyString(), any(Pageable.class)))
                .thenReturn(page);

        Page<Producto> resultado = service.buscarProductosFabricablesAutocomplete("  ps ", pageable);

        assertThat(resultado.getContent()).containsExactly(producto);

        ArgumentCaptor<List<TipoCategoria>> tiposCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> termCaptor = ArgumentCaptor.forClass(String.class);
        verify(productoRepository).buscarFabricablesAutocomplete(tiposCaptor.capture(), termCaptor.capture(), any(Pageable.class));

        assertThat(tiposCaptor.getValue())
                .containsExactlyInAnyOrder(
                        TipoCategoria.PRODUCTO_TERMINADO,
                        TipoCategoria.PRODUCTO_SEMI_ELABORADO
                );
        assertThat(termCaptor.getValue()).isEqualTo("ps");
    }

    @Test
    @DisplayName("buscarOpciones debe normalizar el término y respetar filtro activo")
    void buscarOpciones_normalizaTerminoYFiltraActivos() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("nombre").ascending());
        Producto producto = new Producto();
        Page<Producto> page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.buscarPorTexto(anyString(), any(Boolean.class), any(Pageable.class)))
                .thenReturn(page);

        Page<ProductoOptionDTO> resultado = service.buscarOpciones("  RVC ", true, null, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        ArgumentCaptor<String> termCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> activoCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(productoRepository).buscarPorTexto(termCaptor.capture(), activoCaptor.capture(), any(Pageable.class));
        assertThat(termCaptor.getValue()).isEqualTo("RVC");
        assertThat(activoCaptor.getValue()).isTrue();
    }

    @Test
    @DisplayName("buscarOpciones debe permitir consultas sin término y sin filtro de estado")
    void buscarOpciones_sinTerminoNoFiltraEstado() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("codigoSku"));
        when(productoRepository.buscarPorTexto(null, null, pageable))
                .thenReturn(Page.empty(pageable));

        Page<ProductoOptionDTO> resultado = service.buscarOpciones(null, null, null, pageable);

        assertThat(resultado).isEmpty();
        verify(productoRepository).buscarPorTexto(null, null, pageable);
    }

    @Test
    @DisplayName("buscarOpciones con almacenId usa búsqueda por lotes de almacén")
    void buscarOpciones_conAlmacenIdUsaBusquedaParaConteo() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("nombre"));
        Producto producto = new Producto();
        Page<Producto> page = new PageImpl<>(List.of(producto), pageable, 1);
        when(productoRepository.buscarParaConteo(anyString(), anyLong(), any(Pageable.class)))
                .thenReturn(page);

        Page<ProductoOptionDTO> resultado = service.buscarOpciones("  Lote ", null, 4L, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        ArgumentCaptor<String> termCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> almacenCaptor = ArgumentCaptor.forClass(Long.class);
        verify(productoRepository).buscarParaConteo(termCaptor.capture(), almacenCaptor.capture(), any(Pageable.class));
        assertThat(termCaptor.getValue()).isEqualTo("Lote");
        assertThat(almacenCaptor.getValue()).isEqualTo(4L);
    }

    private static Stream<Arguments> banderasCalidad() {
        return Stream.of(
                Arguments.of(false, false, false),
                Arguments.of(true, false, false),
                Arguments.of(true, true, false),
                Arguments.of(true, true, true)
        );
    }

    private CategoriaProducto categoria(TipoCategoria tipo) {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(tipo);
        return categoria;
    }
}
