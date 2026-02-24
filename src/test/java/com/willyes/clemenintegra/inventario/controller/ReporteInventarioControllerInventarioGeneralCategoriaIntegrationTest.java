package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
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
class ReporteInventarioControllerInventarioGeneralCategoriaIntegrationTest extends IntegrationTestMySqlContainer {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private AlmacenRepository almacenRepository;
    @Autowired private LoteProductoRepository loteProductoRepository;
    @Autowired private MotivoMovimientoRepository motivoMovimientoRepository;
    @Autowired private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Autowired private MovimientoInventarioRepository movimientoInventarioRepository;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private JavaMailSender javaMailSender;

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void previewYExcelFiltranPorCategoriaProductoId() throws Exception {
        DatosCategoria data = crearDatosCategorias();

        registrarEntrada(data.productoCatA, data.loteCatA, data.almacen, data.usuario, data.motivo, data.tipoDetalle, new BigDecimal("9"));
        registrarEntrada(data.productoCatB, data.loteCatB, data.almacen, data.usuario, data.motivo, data.tipoDetalle, new BigDecimal("5"));

        MvcResult previewResult = mockMvc.perform(get("/api/reportes/inventario-general/preview")
                        .param("fechaCorte", "2026-01-31")
                        .param("categoriaProductoId", data.categoriaA.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode preview = objectMapper.readTree(previewResult.getResponse().getContentAsByteArray());
        List<String> skuPreview = new ArrayList<>();
        for (JsonNode row : preview.get("content")) {
            skuPreview.add(row.get("sku").asText());
        }
        assertThat(skuPreview).contains(data.productoCatA.getCodigoSku()).doesNotContain(data.productoCatB.getCodigoSku());

        MvcResult excelResult = mockMvc.perform(get("/api/reportes/inventario-general")
                        .param("fechaCorte", "2026-01-31")
                        .param("categoriaProductoId", data.categoriaA.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();

        List<String> skuExcel = leerSkusExcel(excelResult.getResponse().getContentAsByteArray());
        assertThat(skuExcel).contains(data.productoCatA.getCodigoSku()).doesNotContain(data.productoCatB.getCodigoSku());
    }

    private List<String> leerSkusExcel(byte[] bytes) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> skus = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && row.getCell(0) != null) {
                    skus.add(row.getCell(0).getStringCellValue());
                }
            }
            return skus;
        }
    }

    private void registrarEntrada(Producto producto,
                                  LoteProducto lote,
                                  Almacen almacen,
                                  Usuario usuario,
                                  MotivoMovimiento motivo,
                                  TipoMovimientoDetalle tipoDetalle,
                                  BigDecimal cantidad) {
        movimientoInventarioRepository.save(MovimientoInventario.builder()
                .cantidad(cantidad)
                .tipoMovimiento(TipoMovimiento.ENTRADA)
                .clasificacion(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                .fechaIngreso(LocalDateTime.of(2026, 1, 20, 10, 0))
                .registradoPor(usuario)
                .producto(producto)
                .lote(lote)
                .almacenOrigen(almacen)
                .motivoMovimiento(motivo)
                .tipoMovimientoDetalle(tipoDetalle)
                .loteLegacy(false)
                .build());
    }

    private DatosCategoria crearDatosCategorias() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("inv-cat-" + suffix)
                .clave("secret")
                .nombreCompleto("Usuario Inventario Cat")
                .correo("inv-cat-" + suffix + "@example.com")
                .rol(RolUsuario.ROL_JEFE_ALMACENES)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida um = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad " + suffix)
                .simbolo("UN")
                .codigo("UN")
                .build());

        CategoriaProducto categoriaA = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria A " + suffix)
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        CategoriaProducto categoriaB = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria B " + suffix)
                .tipo(TipoCategoria.MATERIAL_EMPAQUE)
                .build());

        Producto productoCatA = productoRepository.save(Producto.builder()
                .codigoSku("SKU-CAT-A-" + suffix)
                .nombre("Producto Cat A " + suffix)
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(um)
                .categoriaProducto(categoriaA)
                .creadoPor(usuario)
                .activo(true)
                .build());

        Producto productoCatB = productoRepository.save(Producto.builder()
                .codigoSku("SKU-CAT-B-" + suffix)
                .nombre("Producto Cat B " + suffix)
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(um)
                .categoriaProducto(categoriaB)
                .creadoPor(usuario)
                .activo(true)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Almacen Cat " + suffix)
                .ubicacion("Bodega Cat")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        LoteProducto loteCatA = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-CAT-A-" + suffix)
                .fechaVencimiento(LocalDateTime.of(2026, 12, 31, 0, 0))
                .stockLote(BigDecimal.ZERO)
                .estado(EstadoLote.DISPONIBLE)
                .producto(productoCatA)
                .almacen(almacen)
                .usuarioLiberador(usuario)
                .build());

        LoteProducto loteCatB = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-CAT-B-" + suffix)
                .fechaVencimiento(LocalDateTime.of(2026, 12, 31, 0, 0))
                .stockLote(BigDecimal.ZERO)
                .estado(EstadoLote.DISPONIBLE)
                .producto(productoCatB)
                .almacen(almacen)
                .usuarioLiberador(usuario)
                .build());

        MotivoMovimiento motivo = motivoMovimientoRepository.save(MotivoMovimiento.builder()
                .motivo(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                .descripcion("Motivo Cat " + suffix)
                .build());

        TipoMovimientoDetalle tipoDetalle = tipoMovimientoDetalleRepository.save(TipoMovimientoDetalle.builder()
                .descripcion("Detalle Cat " + suffix)
                .build());

        return new DatosCategoria(usuario, categoriaA, productoCatA, productoCatB, almacen, loteCatA, loteCatB, motivo, tipoDetalle);
    }

    private record DatosCategoria(Usuario usuario,
                                  CategoriaProducto categoriaA,
                                  Producto productoCatA,
                                  Producto productoCatB,
                                  Almacen almacen,
                                  LoteProducto loteCatA,
                                  LoteProducto loteCatB,
                                  MotivoMovimiento motivo,
                                  TipoMovimientoDetalle tipoDetalle) {}
}
