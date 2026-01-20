package com.willyes.clemenintegra.bom.controller;

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

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class FormulaProductoListadoIntegrationTest extends IntegrationTestMySqlContainer {

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
    void listarFormulasIncluyeSkuYNombreProducto() throws Exception {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("bom-list-user")
                .clave("secret")
                .nombreCompleto("Usuario BOM Listado")
                .correo("bom-list-user@example.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad BOM")
                .simbolo("UBO")
                .codigo("UBO")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria BOM Listado")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto productoUno = productoRepository.save(Producto.builder()
                .codigoSku("SKU-SEL-1")
                .nombre("Producto Selector Uno")
                .descripcionProducto("Producto selector uno")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .activo(true)
                .build());

        Producto productoDos = productoRepository.save(Producto.builder()
                .codigoSku("SKU-SEL-2")
                .nombre("Producto Selector Dos")
                .descripcionProducto("Producto selector dos")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .activo(true)
                .build());

        FormulaProducto formulaUno = FormulaProducto.builder()
                .producto(productoUno)
                .version("V1")
                .estado(EstadoFormula.BORRADOR)
                .fechaCreacion(LocalDateTime.now().minusDays(1))
                .activo(true)
                .creadoPor(usuario)
                .build();

        FormulaProducto formulaDos = FormulaProducto.builder()
                .producto(productoDos)
                .version("V2")
                .estado(EstadoFormula.EN_REVISION)
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .creadoPor(usuario)
                .build();

        formulaProductoRepository.saveAll(List.of(formulaUno, formulaDos));

        mockMvc.perform(get("/api/bom/formulas")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].productoSku", hasItems("SKU-SEL-1", "SKU-SEL-2")))
                .andExpect(jsonPath("$.content[*].productoNombre", hasItems("Producto Selector Uno", "Producto Selector Dos")));
    }
}
