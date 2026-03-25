package com.willyes.clemenintegra.inventario.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProductoEstadoInactivacionIntegrationTest extends IntegrationTestMySqlContainer {

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
    @WithMockUser(authorities = "INV_DECIDE")
    void inactivar_productoReferenciadoEnFormulaActiva_retorna409() throws Exception {
        BaseDatos datos = baseDatos();

        FormulaProducto formulaActiva = FormulaProducto.builder()
                .producto(datos.productoCabecera)
                .version("V2.1")
                .estado(EstadoFormula.APROBADA)
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .creadoPor(datos.usuario)
                .build();
        formulaActiva.setDetalles(List.of(DetalleFormula.builder()
                .formula(formulaActiva)
                .insumo(datos.insumo)
                .unidadMedida(datos.unidad)
                .cantidadNecesaria(BigDecimal.ONE)
                .obligatorio(true)
                .build()));
        formulaProductoRepository.save(formulaActiva);

        mockMvc.perform(patch("/api/productos/{id}/estado", datos.insumo.getId())
                        .contentType("application/json")
                        .content("{\"activo\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REFERENCIADO_EN_FORMULA_ACTIVA"))
                .andExpect(jsonPath("$.details.productoId").value(datos.insumo.getId()))
                .andExpect(jsonPath("$.details.referenciasActivas").value(1));
    }

    private BaseDatos baseDatos() {
        long seed = System.nanoTime();
        String tag = "prod-inac-" + seed;
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("prod-inac-user-" + tag)
                .clave("secret")
                .nombreCompleto("Usuario Producto Inac " + tag)
                .correo("prod-inac-" + tag + "@example.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad Producto Inac " + tag)
                .simbolo("UPI" + Math.abs(seed % 1000))
                .codigo("UPI" + Math.abs(seed % 1000))
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria Producto Inac " + tag)
                .tipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO)
                .build());

        Producto insumo = productoRepository.save(crearProducto("PS-INS-" + tag, "Insumo " + tag, unidad, categoria, usuario));
        Producto cabecera = productoRepository.save(crearProducto("PT-CAB-" + tag, "Cabecera " + tag, unidad, categoria, usuario));
        return new BaseDatos(usuario, unidad, insumo, cabecera);
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

    private record BaseDatos(Usuario usuario, UnidadMedida unidad, Producto insumo, Producto productoCabecera) {
    }
}
