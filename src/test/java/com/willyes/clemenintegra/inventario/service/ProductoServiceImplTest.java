package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.mapper.ProductoMapper;
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

    private ProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductoServiceImpl(
                productoRepository,
                unidadMedidaRepository,
                categoriaProductoRepository,
                usuarioRepository,
                loteProductoRepository,
                movimientoInventarioRepository,
                productoMapper,
                jwtTokenService,
                stockQueryService
        );
    }

    @Test
    void findByCategoriaTipoReturnsEmptyListWhenRepositoryReturnsNull() {
        TipoCategoria tipoEnum = TipoCategoria.MATERIA_PRIMA;
        when(productoRepository.findByCategoriaProducto_Tipo(tipoEnum)).thenReturn(null);
        when(stockQueryService.obtenerStockDisponible(any())).thenReturn(Collections.emptyMap());

        List<ProductoResponseDTO> result = assertDoesNotThrow(() -> service.findByCategoriaTipo(tipoEnum.name()));

        assertTrue(result.isEmpty());
    }
}

