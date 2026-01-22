package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestH2;
import java.math.BigDecimal;
import java.util.UUID;
import org.hamcrest.Matchers;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(authorities = "ROL_JEFE_CALIDAD")
class VidaUtilProductoControllerIntegrationTest extends IntegrationTestH2 {

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

    @MockBean
    private com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    private CategoriaProducto categoriaPt;
    private CategoriaProducto categoriaPs;
    private String skuPt;
    private String skuPs;
    private String nombreBase;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        skuPt = "PT" + suffix;
        skuPs = "PS" + suffix;
        nombreBase = "Citrato " + suffix;
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("tester-vida-util-" + suffix)
                .clave("secret")
                .nombreCompleto("Tester Vida Util")
                .correo("tester-vida-util-" + suffix + "@example.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad Vida Util " + suffix)
                .simbolo(("UV" + suffix).substring(0, 5))
                .codigo(("U" + suffix).substring(0, 4))
                .build());

        categoriaPt = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PT " + suffix)
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        categoriaPs = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PS " + suffix)
                .tipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO)
                .build());

        CategoriaProducto categoriaMp = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("MP " + suffix)
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        productoRepository.save(crearProducto(skuPt, nombreBase + " Producto", usuario, unidad, categoriaPt));
        productoRepository.save(crearProducto(skuPs, nombreBase + " Semielaborado", usuario, unidad, categoriaPs));
        productoRepository.save(crearProducto("MP" + suffix, "Materia Prima " + suffix, usuario, unidad, categoriaMp));
        productoRepository.save(crearProducto("PTX" + suffix, "Otro Producto " + suffix, usuario, unidad, categoriaPt));
    }

    @Test
    void buscarPorSkuAplicaFiltroCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/calidad/vida-util/productos-terminados")
                        .param("search", skuPt.toLowerCase())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].codigoSku").value(skuPt));
    }

    @Test
    void buscarPorNombreAplicaFiltroContiene() throws Exception {
        mockMvc.perform(get("/api/calidad/vida-util/productos-terminados")
                        .param("search", nombreBase.toLowerCase())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].codigoSku",
                        Matchers.containsInAnyOrder(skuPt, skuPs)));
    }

    @Test
    void buscarSinCoincidenciasDevuelveListaVacia() throws Exception {
        mockMvc.perform(get("/api/calidad/vida-util/productos-terminados")
                        .param("search", "zzzz")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    private Producto crearProducto(
            String codigoSku,
            String nombre,
            Usuario usuario,
            UnidadMedida unidad,
            CategoriaProducto categoria) {
        return Producto.builder()
                .codigoSku(codigoSku)
                .nombre(nombre)
                .stockMinimo(BigDecimal.ONE)
                .activo(true)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .build();
    }
}
