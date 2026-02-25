package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.flyway.enabled=false",
        "DB_SECURPASS=dummy",
        "DB_SECURNAME=dummy"
})
class MovimientoInventarioRepositoryFechaTest {

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Usuario usuario;
    private Producto producto;
    private LoteProducto lote;
    private MotivoMovimiento motivoMovimiento;
    private TipoMovimientoDetalle tipoMovimientoDetalle;
    private Almacen almacen;
    private OrdenProduccion ordenProduccion;

    @BeforeEach
    void setUp() {
        usuario = entityManager.persist(Usuario.builder()
                .nombreUsuario("tester")
                .clave("clave")
                .nombreCompleto("Usuario Test")
                .correo("tester@example.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = entityManager.persist(UnidadMedida.builder()
                .nombre("Unidad")
                .nombrePlural("Unidades")
                .simbolo("U")
                .codigo("U01")
                .simboloImpresion("U")
                .build());

        CategoriaProducto categoria = entityManager.persist(CategoriaProducto.builder()
                .nombre("MP")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        producto = entityManager.persist(Producto.builder()
                .codigoSku("SKU-1")
                .nombre("Producto de prueba")
                .stockMinimo(BigDecimal.ONE)
                .stockMinimoProveedor(BigDecimal.ZERO)
                .leadTimeCompraDias(1)
                .leadTimeProduccionDias(1)
                .stockSeguridad(BigDecimal.ZERO)
                .stockMaximoPlaneacion(BigDecimal.ZERO)
                .rendimientoUnidad(BigDecimal.ONE)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .build());

        almacen = entityManager.persist(Almacen.builder()
                .nombre("Principal")
                .ubicacion("Bodega")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        lote = entityManager.persist(LoteProducto.builder()
                .codigoLote("L-001")
                .fechaFabricacion(LocalDateTime.of(2025, 12, 1, 10, 0))
                .fechaVencimiento(LocalDateTime.of(2026, 1, 1, 10, 0))
                .stockLote(BigDecimal.TEN)
                .agotado(false)
                .stockReservado(BigDecimal.ZERO)
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacen)
                .build());

        motivoMovimiento = entityManager.persist(MotivoMovimiento.builder()
                .descripcion("Compra")
                .motivo(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                .build());

        tipoMovimientoDetalle = entityManager.persist(TipoMovimientoDetalle.builder()
                .descripcion("Detalle")
                .build());

        ordenProduccion = entityManager.persist(OrdenProduccion.builder()
                .codigoOrden("OP-TEST-1")
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .cantidadProgramada(new BigDecimal("10.00"))
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .estado(EstadoProduccion.EN_PROCESO)
                .producto(producto)
                .unidadMedida(producto.getUnidadMedida())
                .responsable(usuario)
                .build());
    }

    @Test
    @DisplayName("filtrar por fechaIngreso respeta rango y orden descendente")
    void filtrarPorRangoYOrden() {
        MovimientoInventario movimientoAntiguo = crearMovimiento(
                LocalDateTime.of(2025, 12, 10, 9, 0),
                BigDecimal.valueOf(5)
        );
        MovimientoInventario movimientoReciente = crearMovimiento(
                LocalDateTime.of(2025, 12, 15, 15, 30),
                BigDecimal.valueOf(8)
        );
        // Fuera de rango
        crearMovimiento(LocalDateTime.of(2025, 12, 20, 8, 0), BigDecimal.valueOf(12));

        LocalDateTime inicio = LocalDateTime.of(2025, 12, 9, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2025, 12, 16, 23, 59, 59);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("fechaIngreso").descending());

        Page<MovimientoInventario> page = movimientoInventarioRepository.filtrar(
                inicio, fin, null, null, null, null, pageable
        );

        List<MovimientoInventario> resultados = page.getContent();
        assertThat(resultados)
                .hasSize(2)
                .extracting(MovimientoInventario::getId)
                .containsExactly(movimientoReciente.getId(), movimientoAntiguo.getId());
        assertThat(resultados.get(0).getFechaIngreso()).isAfterOrEqualTo(resultados.get(1).getFechaIngreso());
    }

    @Test
    @DisplayName("filtrar por productoId devuelve solo movimientos del producto solicitado")
    void filtrarPorProductoId() {
        Producto otroProducto = entityManager.persist(Producto.builder()
                .codigoSku("SKU-2")
                .nombre("Producto alterno")
                .stockMinimo(BigDecimal.ONE)
                .stockMinimoProveedor(BigDecimal.ZERO)
                .leadTimeCompraDias(1)
                .leadTimeProduccionDias(1)
                .stockSeguridad(BigDecimal.ZERO)
                .stockMaximoPlaneacion(BigDecimal.ZERO)
                .rendimientoUnidad(BigDecimal.ONE)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(producto.getUnidadMedida())
                .categoriaProducto(producto.getCategoriaProducto())
                .creadoPor(usuario)
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .build());

        LoteProducto otroLote = entityManager.persist(LoteProducto.builder()
                .codigoLote("L-002")
                .fechaFabricacion(LocalDateTime.of(2025, 12, 2, 10, 0))
                .fechaVencimiento(LocalDateTime.of(2026, 1, 2, 10, 0))
                .stockLote(BigDecimal.TEN)
                .agotado(false)
                .stockReservado(BigDecimal.ZERO)
                .estado(EstadoLote.DISPONIBLE)
                .producto(otroProducto)
                .almacen(almacen)
                .build());

        MovimientoInventario movimientoProductoObjetivo = crearMovimiento(
                LocalDateTime.of(2025, 12, 12, 11, 0),
                BigDecimal.valueOf(3)
        );

        MovimientoInventario movimientoOtroProducto = MovimientoInventario.builder()
                .cantidad(BigDecimal.valueOf(9))
                .tipoMovimiento(TipoMovimiento.RECEPCION)
                .clasificacion(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                .fechaIngreso(LocalDateTime.of(2025, 12, 12, 12, 0))
                .docReferencia("OC-999")
                .registradoPor(usuario)
                .producto(otroProducto)
                .lote(otroLote)
                .almacenOrigen(almacen)
                .almacenDestino(null)
                .motivoMovimiento(motivoMovimiento)
                .tipoMovimientoDetalle(tipoMovimientoDetalle)
                .build();
        movimientoInventarioRepository.save(movimientoOtroProducto);

        entityManager.flush();
        entityManager.clear();

        Page<MovimientoInventario> page = movimientoInventarioRepository.filtrar(
                LocalDateTime.of(2025, 12, 12, 0, 0),
                LocalDateTime.of(2025, 12, 12, 23, 59, 59),
                producto.getId().longValue(),
                null,
                null,
                null,
                PageRequest.of(0, 10, Sort.by("fechaIngreso").descending())
        );

        assertThat(page.getContent())
                .hasSize(1)
                .extracting(MovimientoInventario::getId)
                .containsExactly(movimientoProductoObjetivo.getId());
    }

    @Test
    @DisplayName("sumarCostoMaterialRealOp aplica signo negativo a devoluciones")
    void sumarCostoMaterialRealOp_devolucionRestaCostoTotal() {
        movimientoInventarioRepository.save(MovimientoInventario.builder()
                .cantidad(new BigDecimal("5"))
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .clasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION)
                .fechaIngreso(LocalDateTime.now().minusHours(2))
                .docReferencia("OP-1")
                .registradoPor(usuario)
                .producto(producto)
                .lote(lote)
                .almacenOrigen(almacen)
                .motivoMovimiento(motivoMovimiento)
                .tipoMovimientoDetalle(tipoMovimientoDetalle)
                .ordenProduccion(ordenProduccion)
                .costoTotalAplicado(new BigDecimal("100.000000"))
                .build());

        movimientoInventarioRepository.save(MovimientoInventario.builder()
                .cantidad(new BigDecimal("1"))
                .tipoMovimiento(TipoMovimiento.ENTRADA)
                .clasificacion(ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION)
                .fechaIngreso(LocalDateTime.now().minusHours(1))
                .docReferencia("OP-1")
                .registradoPor(usuario)
                .producto(producto)
                .lote(lote)
                .almacenDestino(almacen)
                .motivoMovimiento(motivoMovimiento)
                .tipoMovimientoDetalle(tipoMovimientoDetalle)
                .ordenProduccion(ordenProduccion)
                .costoTotalAplicado(new BigDecimal("40.000000"))
                .build());

        entityManager.flush();

        BigDecimal total = movimientoInventarioRepository.sumarCostoMaterialRealOp(ordenProduccion.getId());
        assertThat(total).isEqualByComparingTo("60.000000");
    }

    private MovimientoInventario crearMovimiento(LocalDateTime fechaIngreso, BigDecimal cantidad) {
        MovimientoInventario movimiento = MovimientoInventario.builder()
                .cantidad(cantidad)
                .tipoMovimiento(TipoMovimiento.RECEPCION)
                .clasificacion(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                .fechaIngreso(fechaIngreso)
                .docReferencia("OC-123")
                .registradoPor(usuario)
                .producto(producto)
                .lote(lote)
                .almacenOrigen(almacen)
                .almacenDestino(null)
                .motivoMovimiento(motivoMovimiento)
                .tipoMovimientoDetalle(tipoMovimientoDetalle)
                .build();

        movimientoInventarioRepository.save(movimiento);
        entityManager.flush();
        entityManager.clear();
        return movimiento;
    }
}
