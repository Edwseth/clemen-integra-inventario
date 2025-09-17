package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MovimientoInventarioServiceImplAutoSplitTest {

    @Autowired
    private MovimientoInventarioService movimientoInventarioService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LoteProductoRepository loteProductoRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("operador")
                .clave("secreto")
                .nombreCompleto("Operador Test")
                .correo("operador@test.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .build());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(usuario),
                usuario.getClave(),
                List.of(new SimpleGrantedAuthority(usuario.getRol().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transferenciaAutoSplitRespetaReservasYSaldosPorLote() {
        prepararCatalogosBasicos();
        prepararAlmacenes();
        prepararProductoYOrden();
        prepararSolicitudMovimiento();
        prepararLotesOrigen();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("62500.00"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                null,
                24,
                101L,
                1,
                6,
                null,
                null,
                null,
                null,
                2L,
                77L,
                null,
                33L,
                null,
                null,
                null,
                Boolean.TRUE,
                List.of()
        );

        movimientoInventarioService.registrarMovimiento(dto);

        entityManager.flush();
        entityManager.clear();

        verificarLotesOrigen();
        verificarLotesDestino();
        verificarMovimientosGenerados();
    }

    private void prepararCatalogosBasicos() {
        ejecutarSql("INSERT INTO unidades_medida (id, nombre, simbolo) VALUES (?,?,?)",
                10L, "Kilogramo", "kg");

        ejecutarSql("INSERT INTO categorias_producto (id, nombre, tipo) VALUES (?,?,?)",
                20L, "Materia Prima", "MATERIA_PRIMA");
    }

    private void prepararAlmacenes() {
        ejecutarSql("INSERT INTO almacenes (id, nombre, ubicacion, categoria_almacen, tipo_almacen) VALUES (?,?,?,?,?)",
                1, "MP Principal", "Sede Central", "MATERIA_PRIMA", "PRINCIPAL");
        ejecutarSql("INSERT INTO almacenes (id, nombre, ubicacion, categoria_almacen, tipo_almacen) VALUES (?,?,?,?,?)",
                6, "Bodega Producción", "Planta", "PRODUCTO_TERMINADO", "SATELITE");
    }

    private void prepararProductoYOrden() {
        ejecutarSql("""
                INSERT INTO productos (
                    id, codigo_sku, nombre, descripcion_producto, stock_minimo,
                    stock_minimo_proveedor, rendimiento_unidad, activo, fecha_creacion,
                    tipo_analisis_calidad, unidades_medida_id, categorias_producto_id, usuarios_id
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                24L,
                "SKU-24",
                "Producto 24",
                "Materia prima de prueba",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true,
                Timestamp.valueOf(LocalDateTime.now().minusDays(2)),
                "NINGUNO",
                10L,
                20L,
                usuario.getId());

        ejecutarSql("""
                INSERT INTO ordenes_produccion (
                    id, codigo_orden, lote_produccion, lote_producto_id,
                    fecha_inicio, fecha_fin, cantidad_programada, cantidad_producida,
                    cantidad_producida_acumulada, fecha_ultimo_cierre, estado,
                    producto_id, unidad_medida_id, responsable_id, version
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                33L,
                "OP-33",
                "L-OP-33",
                null,
                Timestamp.valueOf(LocalDateTime.now().minusDays(1)),
                null,
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                "EN_PROCESO",
                24L,
                10L,
                usuario.getId(),
                0L);

        ejecutarSql("INSERT INTO tipos_movimiento_detalle (id, descripcion) VALUES (?, ?)",
                2L, "Transferencia interna a producción");
    }

    private void prepararSolicitudMovimiento() {
        ejecutarSql("""
                INSERT INTO solicitudes_movimiento (
                    id, tipo_movimiento, producto_id, lote_id, cantidad,
                    almacen_origen_id, almacen_destino_id, proveedor_id, orden_compra_id,
                    motivo_movimiento_id, tipo_movimiento_detalle_id, codigo_lote,
                    fecha_vencimiento, orden_produccion_id, usuario_solicitante_id,
                    usuario_responsable_id, estado, fecha_solicitud, fecha_resolucion, observaciones
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                77L,
                "TRANSFERENCIA",
                24L,
                null,
                new BigDecimal("62500.00"),
                1,
                6,
                null,
                null,
                null,
                2L,
                null,
                null,
                33L,
                usuario.getId(),
                usuario.getId(),
                "RESERVADA",
                Timestamp.valueOf(LocalDateTime.now().minusHours(2)),
                null,
                null);
    }

    private void prepararLotesOrigen() {
        insertarLoteOrigen(101L, "FEFO-001", LocalDateTime.now().minusMonths(6),
                LocalDateTime.now().plusDays(10), new BigDecimal("25000.00"),
                new BigDecimal("5000.000000"));
        insertarLoteOrigen(102L, "FEFO-002", LocalDateTime.now().minusMonths(5),
                LocalDateTime.now().plusDays(20), new BigDecimal("20000.00"),
                BigDecimal.ZERO.setScale(6));
        insertarLoteOrigen(103L, "FEFO-003", LocalDateTime.now().minusMonths(4),
                LocalDateTime.now().plusDays(30), new BigDecimal("15000.00"),
                BigDecimal.ZERO.setScale(6));
        insertarLoteOrigen(104L, "FEFO-004", LocalDateTime.now().minusMonths(3),
                LocalDateTime.now().plusDays(40), new BigDecimal("12000.00"),
                BigDecimal.ZERO.setScale(6));

        ejecutarSql("INSERT INTO solicitudes_movimiento_detalle (
                        id, solicitud_movimiento_id, lote_id, cantidad, cantidad_atendida,
                        estado, almacen_origen_id, almacen_destino_id
                    ) VALUES (?,?,?,?,?,?,?,?)",
                901L,
                77L,
                101L,
                new BigDecimal("5000.000000"),
                BigDecimal.ZERO.setScale(6),
                "PENDIENTE",
                1,
                6);
    }

    private void verificarLotesOrigen() {
        LoteProducto lote1 = loteProductoRepository.findById(101L).orElseThrow();
        LoteProducto lote2 = loteProductoRepository.findById(102L).orElseThrow();
        LoteProducto lote3 = loteProductoRepository.findById(103L).orElseThrow();
        LoteProducto lote4 = loteProductoRepository.findById(104L).orElseThrow();

        assertEquals(1, lote1.getAlmacen().getId());
        assertEquals(0, lote1.getStockLote().compareTo(new BigDecimal("5000.00")));
        assertEquals(0, lote1.getStockReservado().compareTo(BigDecimal.ZERO.setScale(6)));
        assertFalse(lote1.isAgotado());
        assertNull(lote1.getFechaAgotado());

        assertEquals(1, lote2.getAlmacen().getId());
        assertEquals(0, lote2.getStockLote().compareTo(BigDecimal.ZERO.setScale(2)));
        assertEquals(0, lote2.getStockReservado().compareTo(BigDecimal.ZERO.setScale(6)));
        assertTrue(lote2.isAgotado());
        assertNotNull(lote2.getFechaAgotado());

        assertEquals(0, lote3.getStockLote().compareTo(BigDecimal.ZERO.setScale(2)));
        assertTrue(lote3.isAgotado());
        assertNotNull(lote3.getFechaAgotado());

        assertEquals(0, lote4.getStockLote().compareTo(new BigDecimal("4500.00")));
        assertFalse(lote4.isAgotado());
        assertNull(lote4.getFechaAgotado());
    }

    private void verificarLotesDestino() {
        List<String> codigos = List.of("FEFO-001", "FEFO-002", "FEFO-003", "FEFO-004");
        List<LoteProducto> destinos = codigos.stream()
                .map(codigo -> loteProductoRepository
                        .findByCodigoLoteAndProductoIdAndAlmacenId(codigo, 24, 6)
                        .orElseThrow(() -> new IllegalStateException("No se creó lote destino para " + codigo)))
                .toList();

        assertThat(destinos).hasSize(4);
        assertThat(destinos).allMatch(destino -> destino.getAlmacen().getId() == 6);
        assertThat(destinos).allMatch(destino -> destino.getCodigoLote() != null);
        assertThat(destinos).allMatch(destino -> destino.getStockReservado().compareTo(BigDecimal.ZERO.setScale(6)) == 0);
        assertThat(destinos).allMatch(destino -> !destino.isAgotado());

        assertEquals(0, destinos.stream()
                .filter(destino -> destino.getCodigoLote().equals("FEFO-001"))
                .findFirst()
                .map(LoteProducto::getStockLote)
                .orElse(BigDecimal.ZERO)
                .compareTo(new BigDecimal("20000.00")));

        assertEquals(0, destinos.stream()
                .filter(destino -> destino.getCodigoLote().equals("FEFO-002"))
                .findFirst()
                .map(LoteProducto::getStockLote)
                .orElse(BigDecimal.ZERO)
                .compareTo(new BigDecimal("20000.00")));

        assertEquals(0, destinos.stream()
                .filter(destino -> destino.getCodigoLote().equals("FEFO-003"))
                .findFirst()
                .map(LoteProducto::getStockLote)
                .orElse(BigDecimal.ZERO)
                .compareTo(new BigDecimal("15000.00")));

        assertEquals(0, destinos.stream()
                .filter(destino -> destino.getCodigoLote().equals("FEFO-004"))
                .findFirst()
                .map(LoteProducto::getStockLote)
                .orElse(BigDecimal.ZERO)
                .compareTo(new BigDecimal("7500.00")));
    }

    private void verificarMovimientosGenerados() {
        List<MovimientoInventario> movimientos = movimientoInventarioRepository.findAll();
        assertThat(movimientos).hasSize(4);
        assertThat(movimientos).allMatch(mov -> mov.getTipoMovimiento() == TipoMovimiento.TRANSFERENCIA);
        assertThat(movimientos).allMatch(mov -> mov.getClasificacion() == ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION);
        assertThat(movimientos).allMatch(mov -> mov.getAlmacenOrigen().getId() == 1);
        assertThat(movimientos).allMatch(mov -> mov.getAlmacenDestino().getId() == 6);
        assertThat(movimientos).allMatch(mov -> mov.getLote().getAlmacen().getId() == 6);

        List<BigDecimal> cantidades = movimientos.stream()
                .map(MovimientoInventario::getCantidad)
                .map(cantidad -> cantidad.setScale(2, RoundingMode.HALF_UP))
                .collect(Collectors.toList());

        assertThat(cantidades).containsExactlyInAnyOrder(
                new BigDecimal("20000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("7500.00")
        );
    }

    private void insertarLoteOrigen(Long id, String codigo, LocalDateTime fechaFabricacion,
                                    LocalDateTime fechaVencimiento, BigDecimal stock, BigDecimal reservado) {
        ejecutarSql("""
                INSERT INTO lotes_productos (
                    id, codigo_lote, fecha_fabricacion, fecha_vencimiento,
                    stock_lote, agotado, stock_reservado, fecha_agotado, estado,
                    temperatura_almacenamiento, fecha_liberacion, productos_id, almacenes_id,
                    usuarios_liberador_id, orden_produccion_id, produccion_id
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                id,
                codigo,
                Timestamp.valueOf(fechaFabricacion),
                Timestamp.valueOf(fechaVencimiento),
                stock,
                false,
                reservado,
                null,
                "DISPONIBLE",
                null,
                null,
                24L,
                1,
                null,
                null,
                null);
    }

    private void ejecutarSql(String sql, Object... params) {
        var query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        query.executeUpdate();
    }
}

