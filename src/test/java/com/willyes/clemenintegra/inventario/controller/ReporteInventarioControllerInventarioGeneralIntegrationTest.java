package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UbicacionFisica;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.UbicacionFisicaRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ReporteInventarioControllerInventarioGeneralIntegrationTest extends IntegrationTestMySqlContainer {

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
    private UbicacionFisicaRepository ubicacionFisicaRepository;
    @Autowired
    private LoteProductoRepository loteProductoRepository;
    @Autowired
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Autowired
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void inventarioGeneralIncluyeEntradaAntesCorteYExcluyeSalidaDespues() throws Exception {
        TestData data = crearData();
        registrarMovimiento(data, data.loteA, data.almacenA, null,
                TipoMovimiento.ENTRADA, ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                new BigDecimal("10"), LocalDateTime.of(2026, 1, 20, 8, 0));
        registrarMovimiento(data, data.loteA, data.almacenA, null,
                TipoMovimiento.SALIDA, ClasificacionMovimientoInventario.SALIDA_CLIENTE,
                new BigDecimal("4"), LocalDateTime.of(2026, 2, 1, 8, 0));

        List<FilaExcel> filas = ejecutarYLeer(LocalDate.of(2026, 1, 31));

        FilaExcel fila = filas.stream()
                .filter(f -> f.lote().equals(data.loteA.getCodigoLote()) && f.ubicacion().contains(data.almacenA.getNombre()))
                .findFirst().orElseThrow();
        assertThat(fila.cantidad()).isEqualByComparingTo("10.00");
    }

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void inventarioGeneralTransferenciaCruzandoCorteMantieneSignoPorAlmacen() throws Exception {
        TestData data = crearData();
        registrarMovimiento(data, data.loteA, data.almacenA, data.almacenB,
                TipoMovimiento.TRANSFERENCIA, ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                new BigDecimal("7"), LocalDateTime.of(2026, 2, 1, 10, 0));

        List<FilaExcel> filas = ejecutarYLeer(LocalDate.of(2026, 1, 31));
        assertThat(filas.stream().anyMatch(f -> f.lote().equals(data.loteA.getCodigoLote()))).isFalse();

        registrarMovimiento(data, data.loteA, data.almacenA, data.almacenB,
                TipoMovimiento.TRANSFERENCIA, ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                new BigDecimal("7"), LocalDateTime.of(2026, 1, 30, 10, 0));

        filas = ejecutarYLeer(LocalDate.of(2026, 1, 31));
        FilaExcel destino = filas.stream()
                .filter(f -> f.lote().equals(data.loteA.getCodigoLote()) && f.ubicacion().contains(data.almacenB.getNombre()))
                .findFirst().orElseThrow();
        assertThat(destino.cantidad()).isEqualByComparingTo("7.00");
    }

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void inventarioGeneralIgnoraMovimientosDespuesDelCorte() throws Exception {
        TestData data = crearData();
        registrarMovimiento(data, data.loteB, data.almacenB, null,
                TipoMovimiento.ENTRADA, ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                new BigDecimal("5"), LocalDateTime.of(2026, 2, 2, 10, 0));

        List<FilaExcel> filas = ejecutarYLeer(LocalDate.of(2026, 1, 31));
        assertThat(filas.stream().anyMatch(f -> f.lote().equals(data.loteB.getCodigoLote()))).isFalse();
    }


    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void previewInventarioGeneralRetornaMismasFilasQueExcelYPagina() throws Exception {
        TestData data = crearData();
        registrarMovimiento(data, data.loteA, data.almacenA, null,
                TipoMovimiento.ENTRADA, ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                new BigDecimal("10"), LocalDateTime.of(2026, 1, 20, 8, 0));
        registrarMovimiento(data, data.loteA, data.almacenA, null,
                TipoMovimiento.SALIDA, ClasificacionMovimientoInventario.SALIDA_CLIENTE,
                new BigDecimal("4"), LocalDateTime.of(2026, 2, 1, 8, 0));

        LocalDate fechaCorte = LocalDate.of(2026, 1, 31);
        List<FilaExcel> excelFilas = ejecutarYLeer(fechaCorte);
        List<FilaJson> previewFilas = ejecutarPreviewYLeer(fechaCorte, 0, 20);

        assertThat(previewFilas).hasSize(excelFilas.size());
        FilaExcel filaExcel = excelFilas.stream()
                .filter(f -> f.lote().equals(data.loteA.getCodigoLote()) && f.ubicacion().contains(data.almacenA.getNombre()))
                .findFirst().orElseThrow();
        FilaJson filaPreview = previewFilas.stream()
                .filter(f -> f.lote().equals(data.loteA.getCodigoLote()) && f.ubicacion().contains(data.almacenA.getNombre()))
                .findFirst().orElseThrow();

        assertThat(filaPreview.sku()).isEqualTo(filaExcel.sku());
        assertThat(filaPreview.nombre()).isEqualTo(filaExcel.nombre());
        assertThat(filaPreview.udm()).isEqualTo(filaExcel.udm());
        assertThat(filaPreview.cant()).isEqualByComparingTo(filaExcel.cantidad());
        assertThat(filaPreview.lote()).isEqualTo(filaExcel.lote());
        assertThat(filaPreview.vence()).isEqualTo(filaExcel.vence());
        assertThat(filaPreview.ubicacion()).isEqualTo(filaExcel.ubicacion());
    }

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void previewInventarioGeneralIgnoraMovimientosPosterioresAlCorte() throws Exception {
        TestData data = crearData();
        registrarMovimiento(data, data.loteB, data.almacenB, null,
                TipoMovimiento.ENTRADA, ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                new BigDecimal("5"), LocalDateTime.of(2026, 2, 2, 10, 0));

        List<FilaJson> filas = ejecutarPreviewYLeer(LocalDate.of(2026, 1, 31), 0, 20);
        assertThat(filas.stream().anyMatch(f -> f.lote().equals(data.loteB.getCodigoLote()))).isFalse();
    }


    private List<FilaJson> ejecutarPreviewYLeer(LocalDate fechaCorte, int page, int size) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reportes/inventario-general/preview")
                        .param("fechaCorte", fechaCorte.toString())
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return objectMapper.convertValue(root.get("content"), new TypeReference<List<FilaJson>>() {
        });
    }

    private List<FilaExcel> ejecutarYLeer(LocalDate fechaCorte) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reportes/inventario-general")
                        .param("fechaCorte", fechaCorte.toString()))
                .andExpect(status().isOk())
                .andReturn();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            List<FilaExcel> filas = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                filas.add(new FilaExcel(
                        row.getCell(0).getStringCellValue(),
                        row.getCell(1).getStringCellValue(),
                        row.getCell(2).getStringCellValue(),
                        BigDecimal.valueOf(row.getCell(3).getNumericCellValue()).setScale(2, java.math.RoundingMode.HALF_UP),
                        row.getCell(4).getStringCellValue(),
                        row.getCell(5).getStringCellValue(),
                        row.getCell(6).getStringCellValue()
                ));
            }
            return filas;
        }
    }

    private void registrarMovimiento(TestData data,
                                     LoteProducto lote,
                                     Almacen origen,
                                     Almacen destino,
                                     TipoMovimiento tipo,
                                     ClasificacionMovimientoInventario clasificacion,
                                     BigDecimal cantidad,
                                     LocalDateTime fechaIngreso) {
        movimientoInventarioRepository.save(MovimientoInventario.builder()
                .cantidad(cantidad)
                .tipoMovimiento(tipo)
                .clasificacion(clasificacion)
                .fechaIngreso(fechaIngreso)
                .registradoPor(data.usuario)
                .producto(data.producto)
                .lote(lote)
                .almacenOrigen(origen)
                .almacenDestino(destino)
                .motivoMovimiento(data.motivo)
                .tipoMovimientoDetalle(data.tipoDetalle)
                .loteLegacy(false)
                .build());
    }

    private TestData crearData() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("inv-user-" + suffix)
                .clave("secret")
                .nombreCompleto("Usuario Inventario")
                .correo("inv-user-" + suffix + "@example.com")
                .rol(RolUsuario.ROL_JEFE_ALMACENES)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida um = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad " + suffix)
                .simbolo("UN")
                .codigo("UN")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria " + suffix)
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-INV-" + suffix)
                .nombre("Producto Inv " + suffix)
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(um)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .activo(true)
                .build());

        Almacen almacenA = almacenRepository.save(Almacen.builder()
                .nombre("Almacen A " + suffix)
                .ubicacion("Bodega A")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        Almacen almacenB = almacenRepository.save(Almacen.builder()
                .nombre("Almacen B " + suffix)
                .ubicacion("Bodega B")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        UbicacionFisica ubicA = ubicacionFisicaRepository.save(UbicacionFisica.builder()
                .almacen(almacenA)
                .codigo("R1")
                .descripcion("Rack 1")
                .activo(true)
                .usuario(usuario)
                .build());

        LoteProducto loteA = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-A-" + suffix)
                .fechaVencimiento(LocalDateTime.of(2026, 12, 31, 0, 0))
                .stockLote(BigDecimal.ZERO)
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacenA)
                .ubicacionFisica(ubicA)
                .usuarioLiberador(usuario)
                .build());

        LoteProducto loteB = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-B-" + suffix)
                .fechaVencimiento(LocalDateTime.of(2026, 12, 31, 0, 0))
                .stockLote(BigDecimal.ZERO)
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacenB)
                .usuarioLiberador(usuario)
                .build());

        MotivoMovimiento motivo = motivoMovimientoRepository.save(MotivoMovimiento.builder()
                .motivo(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                .descripcion("Motivo " + suffix)
                .build());

        TipoMovimientoDetalle tipoDetalle = tipoMovimientoDetalleRepository.save(TipoMovimientoDetalle.builder()
                .descripcion("Detalle " + suffix)
                .build());

        return new TestData(usuario, producto, almacenA, almacenB, loteA, loteB, motivo, tipoDetalle);
    }

    private record FilaExcel(String sku, String nombre, String udm, BigDecimal cantidad, String lote, String vence,
                             String ubicacion) {}

    private record FilaJson(String sku, String nombre, String udm, BigDecimal cant, String lote, String vence,
                            String ubicacion) {}

    private record TestData(Usuario usuario,
                            Producto producto,
                            Almacen almacenA,
                            Almacen almacenB,
                            LoteProducto loteA,
                            LoteProducto loteB,
                            MotivoMovimiento motivo,
                            TipoMovimientoDetalle tipoDetalle) {
    }
}
