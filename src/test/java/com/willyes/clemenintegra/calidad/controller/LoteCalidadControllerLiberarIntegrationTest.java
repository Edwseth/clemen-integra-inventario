package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoteCalidadControllerLiberarIntegrationTest extends IntegrationTestMySqlContainer {

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
    private OrdenProduccionRepository ordenProduccionRepository;
    @Autowired
    private LoteProductoRepository loteProductoRepository;
    @Autowired
    private com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository evaluacionCalidadRepository;
    @Autowired
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Autowired
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    private Usuario jefeCalidad;
    private LoteProducto lote;

    @BeforeEach
    void setUp() {
        tipoMovimientoDetalleRepository.deleteAll();
        motivoMovimientoRepository.deleteAll();
        evaluacionCalidadRepository.deleteAll();
        loteProductoRepository.deleteAll();
        ordenProduccionRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaProductoRepository.deleteAll();
        unidadMedidaRepository.deleteAll();
        almacenRepository.deleteAll();
        usuarioRepository.deleteAll();

        jefeCalidad = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("jefe.calidad")
                .clave("secret")
                .nombreCompleto("Jefe Calidad")
                .correo("jefe.calidad@example.com")
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .simbolo("UN")
                .codigo("UND")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PT")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-LIB-1")
                .nombre("Producto Lote Calidad")
                .stockMinimo(BigDecimal.ZERO)
                .activo(true)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(jefeCalidad)
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .build());

        Almacen almacenCuarentena = almacenRepository.save(Almacen.builder()
                .nombre("Cuarentena")
                .ubicacion("Z-CUARENTENA")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        Almacen almacenPrincipal = almacenRepository.save(Almacen.builder()
                .nombre("Principal")
                .ubicacion("Z-PRINCIPAL")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        OrdenProduccion orden = ordenProduccionRepository.save(OrdenProduccion.builder()
                .codigoOrden("OP-LIB-1")
                .loteProduccion("LP-OP-LIB-1")
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducida(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.TEN)
                .estado(EstadoProduccion.EN_PROCESO)
                .producto(producto)
                .unidadMedida(unidad)
                .responsable(jefeCalidad)
                .build());

        lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LP-LIB-1")
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .stockLote(new BigDecimal("5.00"))
                .estado(EstadoLote.EN_CUARENTENA)
                .producto(producto)
                .almacen(almacenCuarentena)
                .ordenProduccion(orden)
                .build());

        evaluacionCalidadRepository.save(EvaluacionCalidad.builder()
                .resultado(ResultadoEvaluacion.CONFORME)
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .fechaEvaluacion(LocalDateTime.now())
                .observaciones("Liberable")
                .archivosAdjuntos(List.of(ArchivoEvaluacion.builder()
                        .nombreArchivo("soporte.pdf")
                        .nombreVisible("Soporte")
                        .build()))
                .loteProducto(lote)
                .usuarioEvaluador(jefeCalidad)
                .build());

        MotivoMovimiento motivo = motivoMovimientoRepository.save(MotivoMovimiento.builder()
                .descripcion("Transferencia por calidad")
                .motivo(ClasificacionMovimientoInventario.LIBERACION_CALIDAD)
                .build());

        TipoMovimientoDetalle tipoDetalle = tipoMovimientoDetalleRepository.save(TipoMovimientoDetalle.builder()
                .descripcion("TRANSFERENCIA")
                .build());

        when(inventoryCatalogResolver.getAlmacenCuarentenaId()).thenReturn(almacenCuarentena.getId().longValue());
        when(inventoryCatalogResolver.resolveAlmacenPrincipal(any())).thenReturn(almacenPrincipal.getId().longValue());
        when(inventoryCatalogResolver.getMotivoIdTransferenciaCalidad()).thenReturn(motivo.getId());
        when(inventoryCatalogResolver.getTipoDetalleTransferenciaId()).thenReturn(tipoDetalle.getId());
    }

    @Test
    void liberarLoteRetornaOkYMapeaOrdenProduccionSinErrorSqlGrammar() throws Exception {
        mockMvc.perform(put("/api/calidad/lotes/{loteId}/liberar", lote.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(jefeCalidad)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacion\":\"Liberación QA aprobada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lote.getId()))
                .andExpect(jsonPath("$.ordenProduccionId").isNumber())
                .andExpect(jsonPath("$.codigoOrdenProduccion").value("OP-LIB-1"));
    }
}
