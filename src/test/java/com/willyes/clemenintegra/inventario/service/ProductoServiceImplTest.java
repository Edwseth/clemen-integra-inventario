package com.willyes.clemenintegra.inventario.service;

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
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    @Mock private JwtTokenService jwtTokenService;
    @Mock private StockQueryService stockQueryService;

    @InjectMocks
    private ProductoServiceImpl service;

    @BeforeEach
    void setUpSecurity() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getCredentials()).thenReturn("token-mock");
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        DefaultClaims claims = new DefaultClaims(Map.of("usuarioId", 1L));
        when(jwtTokenService.extraerClaims("token-mock")).thenReturn(claims);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(new Usuario()));

        when(productoMapper.toDto(any(Producto.class))).thenReturn(new ProductoResponseDTO());
        when(stockQueryService.obtenerStockDisponible(any(Long.class))).thenReturn(BigDecimal.ZERO);
        when(loteProductoRepository.existsByProducto(any(Producto.class))).thenReturn(false);
        when(movimientoInventarioRepository.existsByProductoId(any(Long.class))).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            producto.setId(10);
            return producto;
        });
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
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

        service.crearProducto(dto);

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
        service.crearProducto(dto);
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

        assertThatThrownBy(() -> service.crearProducto(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rendimiento por unidad es obligatorio");
    }
}
