package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AtencionDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MovimientoInventarioServiceIntegrationTest {

    @Autowired
    private MovimientoInventarioService movimientoInventarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AlmacenRepository almacenRepository;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private LoteProductoRepository loteProductoRepository;
    @Autowired
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Autowired
    private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Autowired
    private ReservaLoteRepository reservaLoteRepository;
    @Autowired
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void salidaConSolicitudReservadaDescuentaUnaSolaVez() {
        when(inventoryCatalogResolver.decimals(any())).thenReturn(2);
        when(inventoryCatalogResolver.getTipoDetalleSalidaId()).thenReturn(8L);
        when(inventoryCatalogResolver.getAlmacenPtId()).thenReturn(2L);

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("operador")
                .clave("secreto")
                .nombreCompleto("Operador Principal")
                .correo("operador@example.com")
                .rol(RolUsuario.ROL_ALMACENISTA)
                .activo(true)
                .bloqueado(false)
                .build());

        autenticarUsuario(usuario);

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .simbolo("u")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PT")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-01")
                .nombre("Producto Test")
                .descripcionProducto("Producto de prueba")
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisisCalidad(TipoAnalisisCalidad.NINGUNO)
                .activo(true)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Principal")
                .ubicacion("A1")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        LoteProducto lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-01")
                .producto(producto)
                .almacen(almacen)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("10.00"))
                .stockReservado(new BigDecimal("10.000000"))
                .agotado(false)
                .build());

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .producto(producto)
                .cantidad(new BigDecimal("10.00"))
                .almacenOrigen(almacen)
                .usuarioSolicitante(usuario)
                .usuarioResponsable(usuario)
                .estado(EstadoSolicitudMovimiento.AUTORIZADA)
                .fechaSolicitud(LocalDateTime.now())
                .build();

        SolicitudMovimientoDetalle detalle = SolicitudMovimientoDetalle.builder()
                .solicitudMovimiento(solicitud)
                .lote(lote)
                .cantidad(new BigDecimal("10.000000"))
                .cantidadAtendida(BigDecimal.ZERO.setScale(6))
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .almacenOrigen(almacen)
                .build();
        solicitud.getDetalles().add(detalle);

        solicitud = solicitudMovimientoRepository.saveAndFlush(solicitud);
        SolicitudMovimientoDetalle detallePersistido = solicitud.getDetalles().get(0);

        reservaLoteRepository.save(ReservaLote.builder()
                .lote(lote)
                .solicitudMovimientoDetalle(detallePersistido)
                .cantidadReservada(new BigDecimal("10.000000"))
                .cantidadConsumida(BigDecimal.ZERO.setScale(6))
                .estado(EstadoReservaLote.ACTIVA)
                .build());

        TipoMovimientoDetalle tipoDetalle = tipoMovimientoDetalleRepository.save(TipoMovimientoDetalle.builder()
                .descripcion("Salida regular")
                .build());

        AtencionDTO atencion = new AtencionDTO();
        atencion.setDetalleId(detallePersistido.getId());
        atencion.setLoteId(lote.getId());
        atencion.setCantidad(new BigDecimal("10.000000"));
        atencion.setAlmacenOrigenId(almacen.getId());

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("10.000000"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                null,
                producto.getId(),
                lote.getId(),
                almacen.getId(),
                null,
                null,
                null,
                null,
                tipoDetalle.getId(),
                solicitud.getId(),
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(atencion)
        );

        MovimientoInventarioResponseDTO respuesta = movimientoInventarioService.registrarMovimiento(dto);

        LoteProducto loteActualizado = loteProductoRepository.findById(lote.getId()).orElseThrow();
        SolicitudMovimientoDetalle detalleActualizado = solicitudMovimientoDetalleRepository
                .findById(detallePersistido.getId()).orElseThrow();
        ReservaLote reservaActualizada = reservaLoteRepository.findBySolicitudMovimientoDetalleId(detallePersistido.getId())
                .get(0);

        assertThat(respuesta.getSolicitudId()).isEqualTo(solicitud.getId());
        assertThat(loteActualizado.getStockLote()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(loteActualizado.getStockReservado()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
        assertThat(detalleActualizado.getCantidadAtendida()).isEqualByComparingTo(new BigDecimal("10.000000"));
        assertThat(detalleActualizado.getEstado()).isEqualTo(EstadoSolicitudMovimientoDetalle.ATENDIDO);
        assertThat(reservaActualizada.getEstado()).isEqualTo(EstadoReservaLote.CONSUMIDA);
        assertThat(reservaActualizada.getCantidadConsumida()).isEqualByComparingTo(new BigDecimal("10.000000"));
    }

    private void autenticarUsuario(Usuario usuario) {
        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, usuario.getClave(), userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
