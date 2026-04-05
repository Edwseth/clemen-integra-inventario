package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DetalleFormulaFiltrosIntegrationTest extends IntegrationTestMySqlContainer {

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
    private FormulaProductoRepository formulaProductoRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void listarDetalles_filtraPorFormulaId() throws Exception {
        DatosPrueba datos = sembrarDatos("formula");

        mockMvc.perform(get("/api/bom/detalles")
                        .param("formulaId", String.valueOf(datos.formulaConChontaduro.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[*].formulaId", everyItem(org.hamcrest.Matchers.is(datos.formulaConChontaduro.getId().intValue()))))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void listarDetalles_filtraPorInsumoCaseInsensitiveConTrim() throws Exception {
        sembrarDatos("insumo");

        mockMvc.perform(get("/api/bom/detalles")
                        .param("insumo", "  ChOnTaDuRo  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[*].insumoNombre", everyItem(containsStringIgnoringCase("chontaduro"))))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void listarDetalles_filtraPorFormulaNombreCaseInsensitiveConTrim() throws Exception {
        DatosPrueba datos = sembrarDatos("formulaNombre");

        String terminoFormula = extraerTerminoFormula(datos.formulaConChontaduro.getProducto().getNombre());

        mockMvc.perform(get("/api/bom/detalles")
                        .param("formulaNombre", "  " + terminoFormula.toUpperCase() + "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[*].formulaNombre", everyItem(containsStringIgnoringCase(terminoFormula))))
                .andExpect(jsonPath("$.content[*].formulaNombre", everyItem(org.hamcrest.Matchers.not(containsStringIgnoringCase(
                        datos.formulaSinChontaduro.getProducto().getNombre())))))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void listarDetalles_filtraPorFormulaEInsumo() throws Exception {
        DatosPrueba datos = sembrarDatos("ambos");

        mockMvc.perform(get("/api/bom/detalles")
                        .param("formulaId", String.valueOf(datos.formulaConChontaduro.getId()))
                        .param("insumo", "chontaduro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[*].formulaId", everyItem(org.hamcrest.Matchers.is(datos.formulaConChontaduro.getId().intValue()))))
                .andExpect(jsonPath("$.content[*].insumoNombre", everyItem(containsStringIgnoringCase("chontaduro"))))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    private DatosPrueba sembrarDatos(String sufijo) {
        long seed = System.nanoTime();
        String tag = sufijo + "-" + seed;

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("bom-det-filter-" + tag)
                .clave("secret")
                .nombreCompleto("Usuario BOM Filtros " + tag)
                .correo("bom-det-filter-" + tag + "@example.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad BOM " + tag)
                .simbolo("UB" + tag.substring(0, Math.min(tag.length(), 6)))
                .codigo("COD" + tag.substring(0, Math.min(tag.length(), 6)))
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria BOM " + tag)
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto productoFormulaUno = productoRepository.save(crearProducto("SKU-F-1-" + tag, "Producto Formula Uno " + tag,
                unidad, categoria, usuario));
        Producto productoFormulaDos = productoRepository.save(crearProducto("SKU-F-2-" + tag, "Producto Formula Dos " + tag,
                unidad, categoria, usuario));

        Producto insumoChontaduro = productoRepository.save(crearProducto("SKU-I-1-" + tag, "Pulpa de chontaduro " + tag,
                unidad, categoria, usuario));
        Producto insumoOtro = productoRepository.save(crearProducto("SKU-I-2-" + tag, "Azucar morena " + tag,
                unidad, categoria, usuario));

        FormulaProducto formulaUno = FormulaProducto.builder()
                .producto(productoFormulaUno)
                .version("V1")
                .estado(EstadoFormula.BORRADOR)
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .creadoPor(usuario)
                .build();

        FormulaProducto formulaDos = FormulaProducto.builder()
                .producto(productoFormulaDos)
                .version("V1")
                .estado(EstadoFormula.BORRADOR)
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .creadoPor(usuario)
                .build();

        DetalleFormula detalleChontaduro = DetalleFormula.builder()
                .formula(formulaUno)
                .insumo(insumoChontaduro)
                .unidadMedida(unidad)
                .cantidadNecesaria(BigDecimal.ONE)
                .obligatorio(true)
                .build();

        DetalleFormula detalleOtro = DetalleFormula.builder()
                .formula(formulaDos)
                .insumo(insumoOtro)
                .unidadMedida(unidad)
                .cantidadNecesaria(BigDecimal.TEN)
                .obligatorio(true)
                .build();

        formulaUno.setDetalles(List.of(detalleChontaduro));
        formulaDos.setDetalles(List.of(detalleOtro));

        formulaProductoRepository.saveAll(List.of(formulaUno, formulaDos));

        return new DatosPrueba(formulaUno, formulaDos);
    }

    private Producto crearProducto(String sku, String nombre, UnidadMedida unidad, CategoriaProducto categoria, Usuario usuario) {
        return Producto.builder()
                .codigoSku(sku)
                .nombre(nombre)
                .descripcionProducto(nombre)
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .activo(true)
                .build();
    }

    private String extraerTerminoFormula(String nombreFormula) {
        String prefijo = "Producto Formula Uno ";
        if (nombreFormula != null && nombreFormula.startsWith(prefijo)) {
            return nombreFormula.substring(0, prefijo.length() + 8);
        }
        return "Formula Uno";
    }

    private record DatosPrueba(FormulaProducto formulaConChontaduro, FormulaProducto formulaSinChontaduro) {
    }
}
