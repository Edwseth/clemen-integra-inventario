package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.flyway.enabled=false",
        "DB_SECURPASS=dummy",
        "DB_SECURNAME=dummy"
})
class MovimientoInventarioRepositoryFilterTest {

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AlmacenRepository almacenRepository;

    @Autowired
    private LoteProductoRepository loteProductoRepository;

    @Autowired
    private MotivoMovimientoRepository motivoMovimientoRepository;

    @Autowired
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;

    private Usuario usuario;
    private Producto producto;
    private LoteProducto lote;
    private MotivoMovimiento motivoMovimiento;
    private TipoMovimientoDetalle tipoMovimientoDetalle;

    @BeforeEach
    void setUp() {
        usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("tester")
                .clave("secret")
                .nombreCompleto("Test User")
                .correo("test@example.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .simbolo("U")
                .codigo("U1")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("MP")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-1")
                .nombre("Producto 1")
                .stockMinimo(BigDecimal.ONE)
                .activo(true)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Principal")
                .ubicacion("Lima")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("L-1")
                .fechaFabricacion(LocalDateTime.now())
                .fechaVencimiento(LocalDateTime.now().plusDays(30))
                .stockLote(BigDecimal.TEN)
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacen)
                .build());

        motivoMovimiento = motivoMovimientoRepository.save(MotivoMovimiento.builder()
                .descripcion("Recepción compra")
                .motivo(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                .build());

        tipoMovimientoDetalle = tipoMovimientoDetalleRepository.save(TipoMovimientoDetalle.builder()
                .descripcion("GENERICA")
                .build());
    }

    @Test
    void filtrarPorCodigoRecepcion() {
        crearMovimiento("RC-20251218-OC-001", TipoMovimiento.RECEPCION);
        crearMovimiento("RC-20251218-OC-002", TipoMovimiento.RECEPCION);
        crearMovimiento(null, TipoMovimiento.TRANSFERENCIA);

        Page<MovimientoInventario> page = movimientoInventarioRepository.findAllByCodigoRecepcionAndTipoMovimiento(
                "RC-20251218-OC-001",
                null,
                PageRequest.of(0, 5)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCodigoRecepcion()).contains("RC-20251218-OC-001");
    }

    @Test
    void filtrarPorTipoMovimiento() {
        crearMovimiento("RC-20251218-OC-001", TipoMovimiento.RECEPCION);
        crearMovimiento("RC-20251218-OC-002", TipoMovimiento.RECEPCION);
        crearMovimiento(null, TipoMovimiento.TRANSFERENCIA);

        Page<MovimientoInventario> page = movimientoInventarioRepository.findAllByCodigoRecepcionAndTipoMovimiento(
                null,
                TipoMovimiento.RECEPCION,
                PageRequest.of(0, 5)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(m -> m.getTipoMovimiento() == TipoMovimiento.RECEPCION);
    }

    @Test
    void filtrarPorCodigoRecepcionYTipoMovimiento() {
        crearMovimiento("RC-20251218-OC-001", TipoMovimiento.RECEPCION);
        crearMovimiento("RC-20251218-OC-002", TipoMovimiento.RECEPCION);
        crearMovimiento(null, TipoMovimiento.TRANSFERENCIA);

        Page<MovimientoInventario> page = movimientoInventarioRepository.findAllByCodigoRecepcionAndTipoMovimiento(
                "RC-20251218-OC-002",
                TipoMovimiento.RECEPCION,
                PageRequest.of(0, 5)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCodigoRecepcion()).isEqualTo("RC-20251218-OC-002");
    }

    private MovimientoInventario crearMovimiento(String codigoRecepcion, TipoMovimiento tipoMovimiento) {
        MovimientoInventario movimiento = MovimientoInventario.builder()
                .cantidad(BigDecimal.ONE)
                .tipoMovimiento(tipoMovimiento)
                .clasificacion(null)
                .fechaIngreso(LocalDateTime.now())
                .registradoPor(usuario)
                .producto(producto)
                .lote(lote)
                .motivoMovimiento(motivoMovimiento)
                .tipoMovimientoDetalle(tipoMovimientoDetalle)
                .codigoRecepcion(codigoRecepcion)
                .build();
        return movimientoInventarioRepository.save(movimiento);
    }
}
