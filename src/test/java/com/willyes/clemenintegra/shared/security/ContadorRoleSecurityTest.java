package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.inventario.controller.AjusteInventarioController;
import com.willyes.clemenintegra.inventario.controller.AlertaInventarioController;
import com.willyes.clemenintegra.inventario.controller.CategoriaProductoController;
import com.willyes.clemenintegra.inventario.controller.ConteoCiclicoController;
import com.willyes.clemenintegra.inventario.controller.LoteProductoController;
import com.willyes.clemenintegra.inventario.controller.MovimientoInventarioController;
import com.willyes.clemenintegra.inventario.controller.ProductoController;
import com.willyes.clemenintegra.inventario.controller.SolicitudMovimientoController;
import com.willyes.clemenintegra.inventario.controller.ReporteInventarioController;
import com.willyes.clemenintegra.inventario.controller.OrdenCompraController;
import com.willyes.clemenintegra.inventario.service.OrdenCompraService;
import com.willyes.clemenintegra.inventario.service.RecepcionOCService;
import com.willyes.clemenintegra.inventario.service.HistorialEstadoOrdenService;
import com.willyes.clemenintegra.inventario.service.OrdenCompraPdfService;
import com.willyes.clemenintegra.inventario.service.ReporteInventarioService;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraMapper;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import com.willyes.clemenintegra.planeacion.controller.MrpController;
import com.willyes.clemenintegra.planeacion.controller.PlanProduccionController;
import com.willyes.clemenintegra.planeacion.service.MrpService;
import com.willyes.clemenintegra.planeacion.service.MrpReporteService;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.inventario.dto.AjusteInventarioRequestDTO;
import com.willyes.clemenintegra.inventario.dto.AjusteInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.repository.VidaUtilProductoRepository;
import com.willyes.clemenintegra.inventario.service.AjusteInventarioService;
import com.willyes.clemenintegra.inventario.service.AlertaInventarioService;
import com.willyes.clemenintegra.inventario.service.CategoriaProductoService;
import com.willyes.clemenintegra.inventario.service.ConteoCiclicoService;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import com.willyes.clemenintegra.calidad.controller.NoConformidadController;
import com.willyes.clemenintegra.calidad.controller.RetencionLoteController;
import com.willyes.clemenintegra.calidad.controller.VidaUtilProductoController;
import com.willyes.clemenintegra.calidad.mapper.RetencionLoteMapper;
import com.willyes.clemenintegra.calidad.service.NoConformidadService;
import com.willyes.clemenintegra.calidad.service.RetencionLoteService;
import com.willyes.clemenintegra.calidad.service.VidaUtilProductoService;
import com.willyes.clemenintegra.documental.controller.ControlDocumentalController;
import com.willyes.clemenintegra.documental.dto.DocumentoDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionDTO;
import com.willyes.clemenintegra.documental.service.ControlDocumentalService;
import com.willyes.clemenintegra.produccion.controller.IndicadoresProduccionController;
import com.willyes.clemenintegra.produccion.dto.IndicadoresProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.service.ProduccionIndicadoresService;
import com.willyes.clemenintegra.produccion.service.ReporteIndicadoresProduccionService;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ConteoCiclicoController.class,
        AjusteInventarioController.class,
        SolicitudMovimientoController.class,
        ProductoController.class,
        CategoriaProductoController.class,
        MovimientoInventarioController.class,
        LoteProductoController.class,
        AlertaInventarioController.class,
        ReporteInventarioController.class,
        PlanProduccionController.class,
        MrpController.class,
        OrdenCompraController.class,
        RetencionLoteController.class,
        NoConformidadController.class,
        VidaUtilProductoController.class,
        ControlDocumentalController.class,
        IndicadoresProduccionController.class
})
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, ContadorRoleSecurityTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class ContadorRoleSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConteoCiclicoService conteoCiclicoService;
    @MockBean
    private AjusteInventarioService ajusteInventarioService;
    @MockBean
    private SolicitudMovimientoService solicitudMovimientoService;
    @MockBean
    private ProductoService productoService;
    @MockBean
    private CategoriaProductoService categoriaProductoService;
    @MockBean
    private MovimientoInventarioService movimientoInventarioService;
    @MockBean
    private LoteProductoService loteProductoService;
    @MockBean
    private AlertaInventarioService alertaInventarioService;
    @MockBean
    private ReporteInventarioService reporteInventarioService;
    @MockBean
    private PlanProduccionService planProduccionService;
    @MockBean
    private OrdenProduccionRepository ordenProduccionRepository;
    @MockBean
    private VidaUtilProductoRepository vidaUtilProductoRepository;
    @MockBean
    private MrpService mrpService;
    @MockBean
    private MrpReporteService mrpReporteService;
    @MockBean
    private OrdenCompraService ordenCompraService;
    @MockBean
    private RecepcionOCService recepcionOCService;
    @MockBean
    private HistorialEstadoOrdenService historialEstadoOrdenService;
    @MockBean
    private OrdenCompraPdfService ordenCompraPdfService;
    @MockBean
    private OrdenCompraMapper ordenCompraMapper;
    @MockBean
    private OrdenCompraRepository ordenCompraRepository;
    @MockBean
    private OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    @MockBean
    private ProveedorRepository proveedorRepository;
    @MockBean
    private ProductoRepository productoRepository;
    @MockBean
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @MockBean
    private UnidadMedidaRepository unidadMedidaRepository;
    @MockBean
    private LoteProductoRepository loteProductoRepository;
    @MockBean
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @MockBean
    private StockQueryService stockQueryService;
    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private LoteProductoMapper loteProductoMapper;
    @MockBean
    private EvaluacionCalidadService evaluacionCalidadService;
    @MockBean
    private RetencionLoteService retencionLoteService;
    @MockBean
    private RetencionLoteMapper retencionLoteMapper;
    @MockBean
    private NoConformidadService noConformidadService;
    @MockBean
    private VidaUtilProductoService vidaUtilProductoService;
    @MockBean
    private ControlDocumentalService controlDocumentalService;
    @MockBean
    private ProduccionIndicadoresService produccionIndicadoresService;
    @MockBean
    private ReporteIndicadoresProduccionService reporteIndicadoresProduccionService;
    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private RequestTimingFilter requestTimingFilter;
    @MockBean
    private RequestIdFilter requestIdFilter;
    @MockBean
    private SuperAdminSoloLecturaWriteBlockFilter superAdminSoloLecturaWriteBlockFilter;

    @BeforeEach
    void configureFilters() throws ServletException, IOException {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(usuarioInactivoFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(requestTimingFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(requestIdFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(superAdminSoloLecturaWriteBlockFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @Test
    @WithMockUser(authorities = {"PROD_INDICADORES_READ", "PROD_ALERTAS_READ"})
    void contadorPuedeConsultarIndicadoresYAlertasPeroNoCapas() throws Exception {
        when(produccionIndicadoresService.calcularIndicadores(any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(IndicadoresProduccionResponseDTO.builder().build());
        when(produccionIndicadoresService.obtenerOrdenesConAlertas(any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/produccion/indicadores")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-01-31"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/produccion/ordenes/alertas")
                        .param("fechaReferencia", "2026-01-12"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/calidad/capas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "INV_CONTEOS_READ")
    void contadorPuedeListarConteos() throws Exception {
        when(conteoCiclicoService.listar(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/inventario/conteos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorNoPuedeCrearConteo() throws Exception {
        mockMvc.perform(post("/api/inventario/conteos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorNoPuedeActualizarConteo() throws Exception {
        mockMvc.perform(put("/api/inventario/conteos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROL_CONTADOR", "INV_CONTEOS_APPLY", "INV_CONTEOS_CLOSE"})
    void contadorPuedeAplicarYCerrarConteo() throws Exception {
        when(conteoCiclicoService.aplicar(any(), any())).thenReturn(ConteoCiclicoResponseDTO.builder()
                .id(1L)
                .estado(com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico.APLICADO)
                .build());
        when(conteoCiclicoService.cerrar(any())).thenReturn(ConteoCiclicoResponseDTO.builder()
                .id(1L)
                .estado(com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico.CERRADO)
                .build());

        mockMvc.perform(post("/api/inventario/conteos/1/aplicar"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/inventario/conteos/1/cerrar"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"INV_AJUSTES_READ", "INV_AJUSTES_WRITE"})
    void contadorPuedeListarYAjustarInventario() throws Exception {
        when(ajusteInventarioService.listar(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(ajusteInventarioService.crear(any(AjusteInventarioRequestDTO.class))).thenReturn(
                AjusteInventarioResponseDTO.builder()
                        .id(10L)
                        .fecha(LocalDateTime.now())
                        .motivo("Ajuste")
                        .observaciones("ok")
                        .cantidad(BigDecimal.ONE)
                        .productoNombre("P")
                        .almacenNombre("A")
                        .usuarioNombre("U")
                        .build()
        );

        mockMvc.perform(get("/api/inventario/ajustes"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/inventario/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "motivo":"Ajuste",
                                  "observaciones":"ok",
                                  "cantidad":1,
                                  "tipoAjuste":"POSITIVO",
                                  "productoId":1,
                                  "almacenId":1,
                                  "loteProductoId":1
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void compradorNoPuedeConsultarMovimientosFiltrados() throws Exception {
        mockMvc.perform(get("/api/movimientos/filtrar")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {
            "ROL_CONTADOR",
            // /api/lotes/** exige INV_READ|INV_WRITE en SecurityConfig antes del matcher específico INV_LOTES_READ.
            "INV_READ",
            "INV_PRODUCT_READ",
            "INV_CATEGORIAS_READ",
            "INV_LOTES_READ",
            "INV_ALERTAS_READ"
    })
    void contadorNoPuedeLeerMovimientosSinPermisoDedicado() throws Exception {
        when(productoService.listarTodos(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(categoriaProductoService.listarTodas()).thenReturn(List.of());
        when(loteProductoService.listarTodos(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(alertaInventarioService.obtenerAlertasInventario(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/movimientos/filtrar")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-01-31"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/lotes").param("vencidos", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/inventario/alertas").param("diasVencimiento", "30"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void contadorPuedeExportarReportesInventarioYRotacion() throws Exception {
        when(reporteInventarioService.generarReporteAltaRotacion(any(), any())).thenReturn(new org.apache.poi.xssf.usermodel.XSSFWorkbook());
        when(reporteInventarioService.generarReporteBajaRotacion(any(), any())).thenReturn(new org.apache.poi.xssf.usermodel.XSSFWorkbook());
        when(productoService.generarReporteStockDisponibleExcel()).thenReturn(new org.apache.poi.xssf.usermodel.XSSFWorkbook());
        when(movimientoInventarioService.generarReporteMovimientosExcel(any(), any())).thenReturn(new org.apache.poi.xssf.usermodel.XSSFWorkbook());

        mockMvc.perform(get("/api/reportes/alta-rotacion")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-01-31"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reportes/baja-rotacion")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-01-31"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reportes/stock-disponible"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reportes/movimientos")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "PO_PLAN_SEMANAL_READ")
    void contadorPuedeConsultarPlanSemanalPeroNoMutarlo() throws Exception {
        when(planProduccionService.listar(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(planProduccionService.buscarPorId(1L)).thenReturn(Optional.of(new PlanProduccionSemanal()));

        mockMvc.perform(get("/api/planeacion/planes-semanales"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/planeacion/planes-semanales/1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/planeacion/planes-semanales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/planeacion/planes-semanales/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/planeacion/planes-semanales/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/planeacion/planes-semanales/1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/mrp/corridas/1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/ordenes-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(authorities = "DOC_WRITE")
    void contadorPuedeCrearYSubirControlDocumentalPeroNoEliminar() throws Exception {
        when(controlDocumentalService.crearDocumento(any(), any())).thenReturn(DocumentoDTO.builder().build());
        when(controlDocumentalService.agregarVersion(any(), any(), any(), any())).thenReturn(DocumentoVersionDTO.builder().build());

        mockMvc.perform(post("/api/documental/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"DOC-1\",\"nombre\":\"Manual\",\"tipo\":\"PROCEDIMIENTO\",\"area\":\"CALIDAD\"}"))
                .andExpect(status().isOk());

        MockMultipartFile archivo = new MockMultipartFile("archivo", "doc.txt", "text/plain", "ok".getBytes());
        mockMvc.perform(multipart("/api/documental/documentos/1/versiones")
                        .file(archivo))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/documental/documentos/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorNoPuedeConsultarRetencionesNiNoConformidadesNiVidaUtil() throws Exception {
        mockMvc.perform(get("/api/calidad/retenciones"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/calidad/no-conformidades"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/calidad/vida-util/productos-terminados"))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(authorities = {"ROL_CONTADOR", "INV_MOVIMIENTOS_READ"})
    void contadorConPermisoDedicadoPuedeListarMovimientos() throws Exception {
        when(movimientoInventarioService.filtrar(any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/movimientos/filtrar")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROL_CONTADOR", "INV_SOLICITUDES_READ"})
    void contadorConPermisoDedicadoPuedeListarSolicitudes() throws Exception {
        when(solicitudMovimientoService.listarSolicitudes(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/inventario/solicitudes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorNoPuedeListarSolicitudes() throws Exception {
        mockMvc.perform(get("/api/inventario/solicitudes"))
                .andExpect(status().isForbidden());
    }
}
