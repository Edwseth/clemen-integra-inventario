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
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ComponenteImpactoControllerIntegrationTest extends IntegrationTestMySqlContainer {

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
    @WithMockUser(authorities = "BOM_READ")
    void impacto_separaActivasEHistoricas() throws Exception {
        DatosPrueba datos = sembrarDatosConActivaHistorica("impacto-mixto");

        mockMvc.perform(get("/api/bom/componentes/{productoId}/impacto", datos.insumo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.producto.id").value(datos.insumo.getId()))
                .andExpect(jsonPath("$.resumen.referenciasActivas").value(1))
                .andExpect(jsonPath("$.resumen.referenciasHistoricas").value(1))
                .andExpect(jsonPath("$.resumen.totalReferencias").value(2))
                .andExpect(jsonPath("$.resumen.puedeInactivarse").value(false))
                .andExpect(jsonPath("$.resumen.motivoBloqueo").value("REFERENCIADO_EN_FORMULA_ACTIVA"))
                .andExpect(jsonPath("$.formulasActivas[0].formulaEstado").value("APROBADA"))
                .andExpect(jsonPath("$.formulasActivas[0].formulaActivo").value(true))
                .andExpect(jsonPath("$.formulasHistoricas[0].formulaEstado").value("RECHAZADA"));
    }

    @Test
    @WithMockUser(authorities = "BOM_READ")
    void impacto_conSoloHistoricas_permiteInactivar() throws Exception {
        DatosPrueba datos = sembrarDatosSoloHistorico("impacto-historico");

        mockMvc.perform(get("/api/bom/componentes/{productoId}/impacto", datos.insumo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.referenciasActivas").value(0))
                .andExpect(jsonPath("$.resumen.referenciasHistoricas").value(1))
                .andExpect(jsonPath("$.resumen.totalReferencias").value(1))
                .andExpect(jsonPath("$.resumen.puedeInactivarse").value(true))
                .andExpect(jsonPath("$.resumen.motivoBloqueo").doesNotExist())
                .andExpect(jsonPath("$.formulasActivas").isEmpty())
                .andExpect(jsonPath("$.formulasHistoricas[0].formulaActivo").value(false));
    }

    private DatosPrueba sembrarDatosConActivaHistorica(String tag) {
        BaseDatos base = base(tag);

        FormulaProducto activa = FormulaProducto.builder()
                .producto(base.productoCabeceraActiva)
                .version("V2.1")
                .estado(EstadoFormula.APROBADA)
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .creadoPor(base.usuario)
                .build();
        activa.setDetalles(List.of(DetalleFormula.builder()
                .formula(activa)
                .insumo(base.insumo)
                .unidadMedida(base.unidad)
                .cantidadNecesaria(BigDecimal.ONE)
                .obligatorio(true)
                .build()));

        FormulaProducto historica = FormulaProducto.builder()
                .producto(base.productoCabeceraHistorica)
                .version("V1.0")
                .estado(EstadoFormula.RECHAZADA)
                .fechaCreacion(LocalDateTime.now())
                .activo(false)
                .creadoPor(base.usuario)
                .build();
        historica.setDetalles(List.of(DetalleFormula.builder()
                .formula(historica)
                .insumo(base.insumo)
                .unidadMedida(base.unidad)
                .cantidadNecesaria(BigDecimal.TEN)
                .obligatorio(true)
                .build()));

        formulaProductoRepository.saveAll(List.of(activa, historica));
        return new DatosPrueba(base.insumo);
    }

    private DatosPrueba sembrarDatosSoloHistorico(String tag) {
        BaseDatos base = base(tag);

        FormulaProducto historica = FormulaProducto.builder()
                .producto(base.productoCabeceraHistorica)
                .version("V1.0")
                .estado(EstadoFormula.RECHAZADA)
                .fechaCreacion(LocalDateTime.now())
                .activo(false)
                .creadoPor(base.usuario)
                .build();
        historica.setDetalles(List.of(DetalleFormula.builder()
                .formula(historica)
                .insumo(base.insumo)
                .unidadMedida(base.unidad)
                .cantidadNecesaria(BigDecimal.ONE)
                .obligatorio(true)
                .build()));

        formulaProductoRepository.save(historica);
        return new DatosPrueba(base.insumo);
    }

    private BaseDatos base(String prefijo) {
        long seed = System.nanoTime();
        String tag = prefijo + "-" + seed;
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("impacto-user-" + tag)
                .clave("secret")
                .nombreCompleto("Usuario Impacto " + tag)
                .correo("impacto-" + tag + "@example.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad Impacto " + tag)
                .simbolo("UI" + Math.abs(seed % 1000))
                .codigo("UI" + Math.abs(seed % 1000))
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria Impacto " + tag)
                .tipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO)
                .build());

        Producto insumo = productoRepository.save(crearProducto("PS-INS-" + tag, "Insumo " + tag, unidad, categoria, usuario));
        Producto cabeceraActiva = productoRepository.save(crearProducto("PT-ACT-" + tag, "Cabecera Activa " + tag, unidad, categoria, usuario));
        Producto cabeceraHistorica = productoRepository.save(crearProducto("PT-HIS-" + tag, "Cabecera Historica " + tag, unidad, categoria, usuario));
        return new BaseDatos(usuario, unidad, insumo, cabeceraActiva, cabeceraHistorica);
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

    private record BaseDatos(Usuario usuario,
                             UnidadMedida unidad,
                             Producto insumo,
                             Producto productoCabeceraActiva,
                             Producto productoCabeceraHistorica) {
    }

    private record DatosPrueba(Producto insumo) {
    }
}
