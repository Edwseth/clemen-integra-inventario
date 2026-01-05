package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
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
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProduccionTrazabilidadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrdenProduccionRepository ordenProduccionRepository;
    @Autowired
    private EtapaProduccionRepository etapaProduccionRepository;
    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Autowired
    private LoteProductoRepository loteProductoRepository;
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
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Autowired
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;

    @MockBean
    private com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    private Usuario usuario;
    private Producto producto;
    private Almacen almacen;
    private LoteProducto loteMp;
    private MotivoMovimiento motivoMovimiento;
    private TipoMovimientoDetalle tipoMovimientoDetalle;

    @BeforeEach
    void setUp() {
        usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("tester")
                .clave("secret")
                .nombreCompleto("Tester")
                .correo("tester@example.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .codigo("U")
                .simbolo("U")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("MP")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-TZ-1")
                .nombre("Insumo Trazabilidad")
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

        almacen = almacenRepository.save(Almacen.builder()
                .nombre("Principal")
                .tipo(TipoAlmacen.PRINCIPAL)
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .ubicacion("Bodega Central")
                .build());

        loteMp = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("L-TZ-MP-1")
                .producto(producto)
                .almacen(almacen)
                .estado(com.willyes.clemenintegra.inventario.model.enums.EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("50"))
                .stockReservado(BigDecimal.ZERO)
                .build());

        motivoMovimiento = motivoMovimientoRepository.save(MotivoMovimiento.builder()
                .descripcion("Consumo")
                .motivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION)
                .build());

        tipoMovimientoDetalle = tipoMovimientoDetalleRepository.save(TipoMovimientoDetalle.builder()
                .descripcion("Consumo Prod")
                .build());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void retornaMatrizDeTrazabilidadConConsumosYLotes() throws Exception {
        OrdenProduccion orden = ordenProduccionRepository.save(OrdenProduccion.builder()
                .codigoOrden("OP-TZ-1")
                .producto(producto)
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(new BigDecimal("10"))
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .estado(EstadoProduccion.EN_PROCESO)
                .build());

        EtapaProduccion etapa = etapaProduccionRepository.save(EtapaProduccion.builder()
                .nombre("Mezclado")
                .secuencia(1)
                .estado(EstadoEtapa.EN_PROCESO)
                .ordenProduccion(orden)
                .build());

        LoteProducto lotePt = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("L-TZ-PT-1")
                .producto(producto)
                .almacen(almacen)
                .estado(com.willyes.clemenintegra.inventario.model.enums.EstadoLote.DISPONIBLE)
                .stockLote(BigDecimal.ZERO)
                .ordenProduccion(orden)
                .build());

        MovimientoInventario movSalida = movimientoInventarioRepository.save(MovimientoInventario.builder()
                .cantidad(new BigDecimal("5"))
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .clasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION)
                .fechaIngreso(LocalDateTime.now().minusHours(1))
                .registradoPor(usuario)
                .producto(producto)
                .lote(loteMp)
                .almacenOrigen(almacen)
                .motivoMovimiento(motivoMovimiento)
                .tipoMovimientoDetalle(tipoMovimientoDetalle)
                .ordenProduccion(orden)
                .ordenProduccionEtapa(etapa)
                .build());

        MovimientoInventario movTransfer = movimientoInventarioRepository.save(MovimientoInventario.builder()
                .cantidad(new BigDecimal("3"))
                .tipoMovimiento(TipoMovimiento.TRANSFERENCIA)
                .clasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION)
                .fechaIngreso(LocalDateTime.now())
                .registradoPor(usuario)
                .producto(producto)
                .lote(loteMp)
                .almacenOrigen(almacen)
                .motivoMovimiento(motivoMovimiento)
                .tipoMovimientoDetalle(tipoMovimientoDetalle)
                .ordenProduccion(orden)
                .ordenProduccionEtapa(etapa)
                .build());

        mockMvc.perform(get("/api/produccion/ordenes/{id}/trazabilidad", orden.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumos").isArray())
                .andExpect(jsonPath("$.consumos[0].movimientoId").value(movSalida.getId()))
                .andExpect(jsonPath("$.consumos[0].etapaId").value(etapa.getId()))
                .andExpect(jsonPath("$.lotesResultantes[0].loteId").value(lotePt.getId()))
                .andExpect(jsonPath("$.movimientos").isArray())
                .andExpect(jsonPath("$.movimientos.length()").value(2));
    }
}
