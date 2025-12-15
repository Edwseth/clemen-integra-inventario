package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.flyway.enabled=false",
        "DB_SECURPASS=dummy",
        "DB_SECURNAME=dummy"
})
class MovimientoInventarioIdempotenciaRepositoryTest {

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
    }

    @Test
    @DisplayName("permite dos movimientos del mismo lote con claves idempotentes distintas")
    void permiteMultiplesMovimientosPorLote() {
        MovimientoInventario primero = crearMovimiento(new BigDecimal("5"), "idempo-1");
        MovimientoInventario segundo = crearMovimiento(new BigDecimal("3"), "idempo-2");

        movimientoInventarioRepository.save(primero);
        movimientoInventarioRepository.save(segundo);
        entityManager.flush();

        List<MovimientoInventario> movimientos = movimientoInventarioRepository.findAll();
        assertThat(movimientos).hasSize(2);
        assertThat(movimientos)
                .extracting(m -> m.getLote().getId())
                .containsOnly(lote.getId());
    }

    @Test
    @DisplayName("rechaza un reintento con la misma Idempotency-Key dejando un solo registro")
    void evitaDuplicadoPorIdempotencia() {
        movimientoInventarioRepository.save(crearMovimiento(BigDecimal.ONE, "repetida"));
        entityManager.flush();

        long countAntes = movimientoInventarioRepository.count();

        assertThatThrownBy(() -> movimientoInventarioRepository.saveAndFlush(crearMovimiento(BigDecimal.TEN, "repetida")))
                .isInstanceOf(DataIntegrityViolationException.class);

        entityManager.clear();
        assertThat(movimientoInventarioRepository.count()).isEqualTo(countAntes);
    }

    private MovimientoInventario crearMovimiento(BigDecimal cantidad, String idempotencyKey) {
        MovimientoInventario movimiento = MovimientoInventario.builder()
                .cantidad(cantidad)
                .tipoMovimiento(TipoMovimiento.RECEPCION)
                .clasificacion(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                .fechaIngreso(LocalDateTime.now())
                .docReferencia("OC-123")
                .registradoPor(usuario)
                .producto(producto)
                .lote(lote)
                .almacenOrigen(almacen)
                .almacenDestino(null)
                .motivoMovimiento(motivoMovimiento)
                .tipoMovimientoDetalle(tipoMovimientoDetalle)
                .idempotencyKey(idempotencyKey)
                .build();

        return movimiento;
    }
}
