package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
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
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportesCalidadExcelIntegrationTest extends IntegrationTestMySqlContainer {

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

    private LoteProducto loteEnCuarentena;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("jefe.calidad")
                .clave("secret")
                .nombreCompleto("Jefe Calidad")
                .correo("jefe.calidad@example.com")
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad Excel")
                .simbolo("UEX")
                .codigo("UEX")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria Excel")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-EXCEL")
                .nombre("Producto Excel")
                .descripcionProducto("Producto excel")
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
                .nombre("Almacen Excel")
                .ubicacion("Zona QA")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        loteEnCuarentena = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-CUARENTENA")
                .fechaFabricacion(LocalDateTime.now().minusDays(2))
                .stockLote(BigDecimal.TEN)
                .estado(EstadoLote.EN_CUARENTENA)
                .producto(producto)
                .almacen(almacen)
                .usuarioLiberador(usuario)
                .build());

        LoteProducto loteLiberado = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-LIBERADO")
                .fechaFabricacion(LocalDateTime.now().minusDays(3))
                .stockLote(BigDecimal.TEN)
                .estado(EstadoLote.LIBERADO)
                .producto(producto)
                .almacen(almacen)
                .usuarioLiberador(usuario)
                .build());

        evaluacionCalidadRepository.save(EvaluacionCalidad.builder()
                .resultado(ResultadoEvaluacion.CONFORME)
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .fechaEvaluacion(LocalDateTime.of(2026, 1, 5, 10, 0))
                .observaciones("Observaciones")
                .archivosAdjuntos(List.of(ArchivoEvaluacion.builder()
                        .nombreArchivo("reporte.pdf")
                        .nombreVisible("reporte visible")
                        .build()))
                .loteProducto(loteEnCuarentena)
                .usuarioEvaluador(usuario)
                .build());

        evaluacionCalidadRepository.save(EvaluacionCalidad.builder()
                .resultado(ResultadoEvaluacion.CONFORME)
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .fechaEvaluacion(LocalDateTime.of(2026, 1, 6, 10, 0))
                .observaciones("Observaciones")
                .archivosAdjuntos(List.of())
                .loteProducto(loteLiberado)
                .usuarioEvaluador(usuario)
                .build());
    }

    @Test
    @WithMockUser(username = "jefe.calidad", authorities = "ROL_JEFE_CALIDAD")
    void exportarExcelFiltraPorEstadoYLazyInit() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/calidad/reportes/evaluaciones/excel")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-01-17")
                        .param("estado", "EN_CUARENTENA"))
                .andExpect(status().isOk())
                .andReturn();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            var dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("LOTE-CUARENTENA");
            assertThat(dataRow.getCell(9).getStringCellValue()).isEqualTo("SI");
        }
    }
}
