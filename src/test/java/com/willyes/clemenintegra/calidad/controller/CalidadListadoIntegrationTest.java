package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoIncidente;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalidadListadoIntegrationTest extends IntegrationTestMySqlContainer {

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
    private EvaluacionCalidadRepository evaluacionCalidadRepository;
    @Autowired
    private NoConformidadRepository noConformidadRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    private Usuario usuario;
    private LoteProducto loteProducto;
    private EvaluacionCalidad evaluacion;
    private NoConformidad noConformidad;

    @BeforeEach
    void setUp() {
        usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("calidad.listado")
                .clave("secret")
                .nombreCompleto("Usuario Calidad Listado")
                .correo("calidad.listado@example.com")
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad Calidad")
                .simbolo("UQ")
                .codigo("UQ")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria Calidad")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-CAL-1")
                .nombre("Producto Calidad")
                .descripcionProducto("Producto calidad")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.FISICO)
                .requiereAnalisisFisico(true)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .activo(true)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Almacen Calidad Listado")
                .ubicacion("Zona QA")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        loteProducto = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LP-CAL-1")
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .stockLote(BigDecimal.TEN)
                .estado(EstadoLote.EN_CUARENTENA)
                .producto(producto)
                .almacen(almacen)
                .usuarioLiberador(usuario)
                .build());

        evaluacion = evaluacionCalidadRepository.save(EvaluacionCalidad.builder()
                .resultado(ResultadoEvaluacion.CONFORME)
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .fechaEvaluacion(LocalDateTime.now())
                .observaciones("OK")
                .archivosAdjuntos(List.of(ArchivoEvaluacion.builder()
                        .nombreArchivo("adjunto-1.pdf")
                        .nombreVisible("Adjunto 1")
                        .build()))
                .loteProducto(loteProducto)
                .usuarioEvaluador(usuario)
                .build());

        noConformidad = noConformidadRepository.save(NoConformidad.builder()
                .codigo("NC-TEST-001")
                .origen(OrigenNoConformidad.LOTE)
                .severidad(SeveridadNoConformidad.MAYOR)
                .tipoIncidente(TipoIncidente.NO_CONFORMIDAD)
                .estado(EstadoNoConformidad.ABIERTA)
                .descripcion("Detalle NC")
                .fechaRegistro(LocalDateTime.now())
                .usuarioReporta(usuario)
                .lote(loteProducto)
                .producto(producto)
                .evaluacion(evaluacion)
                .build());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void listarEvaluacionesConsolidadasIncluyeCantidadAdjuntos() throws Exception {
        LocalDate hoy = LocalDate.now();
        mockMvc.perform(get("/api/calidad/evaluaciones/consolidadas")
                        .param("fechaInicio", hoy.minusDays(1).toString())
                        .param("fechaFin", hoy.plusDays(1).toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigoLote").value("LP-CAL-1"))
                .andExpect(jsonPath("$.content[0].cantidadAdjuntos").value(1))
                .andExpect(jsonPath("$.content[0].tieneAdjuntos").value(true));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void listarNoConformidadesIncluyeNombreUsuario() throws Exception {
        mockMvc.perform(get("/api/calidad/no-conformidades")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reportadoPorNombre").value("Usuario Calidad Listado"))
                .andExpect(jsonPath("$.content[0].codigo").value("NC-TEST-001"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void obtenerNoConformidadIncluyeNombresRelacionados() throws Exception {
        mockMvc.perform(get("/api/calidad/no-conformidades/{id}", noConformidad.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportadoPorNombre").value("Usuario Calidad Listado"))
                .andExpect(jsonPath("$.productoNombre").value("Producto Calidad"));
    }
}
