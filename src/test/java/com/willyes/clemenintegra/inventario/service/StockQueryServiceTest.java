package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StockQueryServiceTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private StockQueryService stockQueryService;

    @BeforeEach
    void setUp() {
        stockQueryService = new StockQueryService(productoRepository);
    }

    @Test
    void devuelveProductoConStockCeroCuandoNoHayLotesEnAlmacenFiltrado() {
        UnidadMedida unidad = entityManager.persistAndFlush(UnidadMedida.builder()
                .nombre("Unidad")
                .simbolo("u")
                .build());

        CategoriaProducto categoria = entityManager.persistAndFlush(CategoriaProducto.builder()
                .nombre("Insumos")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Usuario usuario = entityManager.persistAndFlush(Usuario.builder()
                .nombreUsuario("operador")
                .clave("secreto")
                .nombreCompleto("Operador Principal")
                .correo("operador@example.com")
                .rol(RolUsuario.ROL_ALMACENISTA)
                .activo(true)
                .bloqueado(false)
                .build());

        Producto producto = new Producto();
        producto.setCodigoSku("INS-001");
        producto.setNombre("Insumo 1");
        producto.setDescripcionProducto("Insumo de prueba");
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setActivo(true);
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.NINGUNO);
        producto.setUnidadMedida(unidad);
        producto.setCategoriaProducto(categoria);
        producto.setCreadoPor(usuario);
        producto = entityManager.persistAndFlush(producto);

        Almacen almacenFiltrado = entityManager.persistAndFlush(Almacen.builder()
                .nombre("Principal")
                .ubicacion("Zona A")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        Almacen almacenAlterno = entityManager.persistAndFlush(Almacen.builder()
                .nombre("Secundario")
                .ubicacion("Zona B")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.SATELITE)
                .build());

        entityManager.persistAndFlush(LoteProducto.builder()
                .codigoLote("LOTE-EXT")
                .producto(producto)
                .almacen(almacenAlterno)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("5.00"))
                .stockReservado(BigDecimal.ZERO.setScale(6))
                .agotado(false)
                .build());

        Long productoId = producto.getId().longValue();
        Long almacenFiltradoId = almacenFiltrado.getId().longValue();

        Map<Long, BigDecimal> stockPorProducto = stockQueryService.obtenerStockDisponible(
                List.of(productoId),
                List.of(almacenFiltradoId)
        );

        assertThat(stockPorProducto).containsOnlyKeys(productoId);
        assertThat(stockPorProducto.get(productoId)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}

