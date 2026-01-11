package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrdenProduccionMovimientosControllerTest extends IntegrationTestMySqlContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private AlmacenRepository almacenRepository;

    @Autowired
    private LoteProductoRepository loteProductoRepository;

    @Autowired
    private MotivoMovimientoRepository motivoMovimientoRepository;

    @Autowired
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    private OrdenProduccionRepository ordenProduccionRepository;

    @Autowired
    private EtapaProduccionRepository etapaProduccionRepository;

    @MockBean
    private com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setup() {
        movimientoInventarioRepository.deleteAll();
        etapaProduccionRepository.deleteAll();
        loteProductoRepository.deleteAll();
        ordenProduccionRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaProductoRepository.deleteAll();
        unidadMedidaRepository.deleteAll();
        almacenRepository.deleteAll();
        usuarioRepository.deleteAll();
        motivoMovimientoRepository.deleteAll();
        tipoMovimientoDetalleRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void listarMovimientosIncluyeDatosDeProductoYLote() throws Exception {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("tester")
                .clave("clave")
                .nombreCompleto("Usuario Tester")
                .correo("tester@example.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidadMedida = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .nombrePlural("Unidades")
                .simbolo("u")
                .codigo("UND")
                .simboloImpresion("u")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PT")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-ENTRADA")
                .nombre("Producto Entrada")
                .stockMinimo(BigDecimal.ZERO)
                .activo(true)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(unidadMedida)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .build());

        OrdenProduccion ordenProduccion = ordenProduccionRepository.save(OrdenProduccion.builder()
                .codigoOrden("OP-100")
                .loteProduccion("LOTE-OP-100")
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(new BigDecimal("10"))
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .estado(EstadoProduccion.EN_PROCESO)
                .producto(producto)
                .unidadMedida(unidadMedida)
                .responsable(usuario)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Principal")
                .ubicacion("Bodega Central")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        LoteProducto lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-123")
                .stockLote(new BigDecimal("50"))
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacen)
                .build());

        MotivoMovimiento motivo = motivoMovimientoRepository.save(MotivoMovimiento.builder()
                .descripcion("Entrada producto terminado")
                .motivo(ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO)
                .build());

        TipoMovimientoDetalle tipoDetalle = tipoMovimientoDetalleRepository.save(
                TipoMovimientoDetalle.builder()
                        .descripcion("Entrada PT")
                        .build()
        );

        movimientoInventarioRepository.save(MovimientoInventario.builder()
                .cantidad(new BigDecimal("5"))
                .tipoMovimiento(TipoMovimiento.ENTRADA)
                .clasificacion(ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO)
                .registradoPor(usuario)
                .producto(producto)
                .lote(lote)
                .almacenDestino(almacen)
                .motivoMovimiento(motivo)
                .tipoMovimientoDetalle(tipoDetalle)
                .ordenProduccion(ordenProduccion)
                .build());

        mockMvc.perform(get("/api/produccion/ordenes/{id}/movimientos", ordenProduccion.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productoId").value(producto.getId()))
                .andExpect(jsonPath("$.content[0].codigoSku").value(producto.getCodigoSku()))
                .andExpect(jsonPath("$.content[0].loteId").value(lote.getId()))
                .andExpect(jsonPath("$.content[0].codigoLote").value(lote.getCodigoLote()))
                .andExpect(jsonPath("$.content[0].nombreProducto").value(producto.getNombre()));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void listarMovimientosIncluyeNombreDeEtapaCuandoExiste() throws Exception {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("tester-etapa")
                .clave("clave")
                .nombreCompleto("Usuario Tester Etapa")
                .correo("tester+etapa@example.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidadMedida = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .nombrePlural("Unidades")
                .simbolo("u")
                .codigo("UND")
                .simboloImpresion("u")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PT")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-ETAPA")
                .nombre("Producto con Etapa")
                .stockMinimo(BigDecimal.ZERO)
                .activo(true)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(unidadMedida)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .build());

        OrdenProduccion ordenProduccion = ordenProduccionRepository.save(OrdenProduccion.builder()
                .codigoOrden("OP-200")
                .loteProduccion("LOTE-OP-200")
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(new BigDecimal("20"))
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .estado(EstadoProduccion.EN_PROCESO)
                .producto(producto)
                .unidadMedida(unidadMedida)
                .responsable(usuario)
                .build());

        EtapaProduccion etapa = etapaProduccionRepository.save(EtapaProduccion.builder()
                .nombre("Acondicionado")
                .secuencia(1)
                .ordenProduccion(ordenProduccion)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Principal")
                .ubicacion("Bodega Central")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        LoteProducto lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-456")
                .stockLote(new BigDecimal("25"))
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacen)
                .build());

        MotivoMovimiento motivo = motivoMovimientoRepository.save(MotivoMovimiento.builder()
                .descripcion("Consumo etapa")
                .motivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION)
                .build());

        TipoMovimientoDetalle tipoDetalle = tipoMovimientoDetalleRepository.save(
                TipoMovimientoDetalle.builder()
                        .descripcion("Consumo etapa")
                        .build()
        );

        movimientoInventarioRepository.save(MovimientoInventario.builder()
                .cantidad(new BigDecimal("7"))
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .clasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION)
                .registradoPor(usuario)
                .producto(producto)
                .lote(lote)
                .almacenOrigen(almacen)
                .motivoMovimiento(motivo)
                .tipoMovimientoDetalle(tipoDetalle)
                .ordenProduccion(ordenProduccion)
                .ordenProduccionEtapa(etapa)
                .build());

        mockMvc.perform(get("/api/produccion/ordenes/{id}/movimientos", ordenProduccion.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ordenProduccionEtapaId").value(etapa.getId()))
                .andExpect(jsonPath("$.content[0].nombreEtapaProduccion").value(etapa.getNombre()));
    }
}
