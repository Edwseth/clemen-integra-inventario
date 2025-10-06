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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.jpa.defer-datasource-initialization=true")
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
    @Autowired
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Autowired
    private OrdenCompraRepository ordenCompraRepository;
    @Autowired
    private OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Autowired
    private RecepcionOCRepository recepcionOCRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void salidaConSolicitudReservadaDescuentaUnaSolaVez() {
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
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
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

    @Test
    void recepcionCompraGeneraCabeceraYCodigo() {
        when(inventoryCatalogResolver.decimals(any())).thenReturn(2);

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("receptor")
                .clave("clave")
                .nombreCompleto("Usuario Recepcion")
                .correo("receptor@example.com")
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
                .nombre("Materia Prima")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());
        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-RC")
                .nombre("Producto Recepcion")
                .descripcionProducto("Producto para recepcion")
                .stockMinimo(BigDecimal.ZERO)
                .stockMinimoProveedor(BigDecimal.ZERO)
                .rendimientoUnidad(BigDecimal.ONE)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .activo(true)
                .build());

        Almacen almacenDestino = almacenRepository.save(Almacen.builder()
                .nombre("Almacen Recepcion")
                .ubicacion("Zona R")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        Proveedor proveedor = proveedorRepository.save(Proveedor.builder()
                .nombre("Proveedor Recepcion")
                .identificacion("999")
                .telefono("555")
                .email("prov@recepcion.com")
                .direccion("Calle 123")
                .paginaWeb("www.prov.com")
                .nombreContacto("Contacto Recepcion")
                .activo(true)
                .build());

        OrdenCompra ordenCompra = ordenCompraRepository.save(OrdenCompra.builder()
                .codigoOrden("OC-RC-1")
                .fechaOrden(LocalDateTime.now())
                .proveedor(proveedor)
                .estado(EstadoOrdenCompra.CREADA)
                .observaciones("Orden de prueba")
                .build());

        OrdenCompraDetalle detalle = ordenCompraDetalleRepository.save(OrdenCompraDetalle.builder()
                .ordenCompra(ordenCompra)
                .producto(producto)
                .cantidad(new BigDecimal("5.000"))
                .valorUnitario(new BigDecimal("10.000"))
                .valorTotal(new BigDecimal("50.000"))
                .iva(BigDecimal.ZERO.setScale(2))
                .cantidadRecibida(BigDecimal.ZERO.setScale(3))
                .build());
        ordenCompra.setDetalles(new ArrayList<>(List.of(detalle)));

        MotivoMovimiento motivo = motivoMovimientoRepository.save(MotivoMovimiento.builder()
                .descripcion("Recepción Compra")
                .motivo(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                .build());

        TipoMovimientoDetalle tipoDetalle = tipoMovimientoDetalleRepository.save(TipoMovimientoDetalle.builder()
                .descripcion("Recepción OC")
                .build());

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("5.000000"),
                TipoMovimiento.RECEPCION,
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "Factura RC-01",
                null,
                producto.getId(),
                null,
                null,
                almacenDestino.getId(),
                proveedor.getId(),
                ordenCompra.getId(),
                motivo.getId(),
                tipoDetalle.getId(),
                null,
                null,
                null,
                detalle.getId(),
                "LOTE-RC-01",
                LocalDateTime.now().plusWeeks(2),
                null,
                Boolean.FALSE,
                List.of()
        );

        MovimientoInventarioResponseDTO respuesta = movimientoInventarioService.registrarMovimiento(dto);

        MovimientoInventario guardado = movimientoInventarioRepository.findById(respuesta.getId()).orElseThrow();
        assertThat(guardado.getRecepcionOc()).isNotNull();
        RecepcionOC cabecera = recepcionOCRepository.findById(guardado.getRecepcionOc().getId()).orElseThrow();

        assertThat(respuesta.getCodigoRecepcion()).isNotBlank();
        assertThat(respuesta.getRecepcionOcId()).isEqualTo(cabecera.getId());
        assertThat(guardado.getCodigoRecepcion()).isEqualTo(cabecera.getCodigo());
        assertThat(cabecera.getOrdenCompra().getId()).isEqualTo(ordenCompra.getId());
        assertThat(cabecera.getFechaRecepcion()).isEqualTo(guardado.getFechaIngreso().toLocalDate());
    }

    private void autenticarUsuario(Usuario usuario) {
        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, usuario.getClave(), userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
