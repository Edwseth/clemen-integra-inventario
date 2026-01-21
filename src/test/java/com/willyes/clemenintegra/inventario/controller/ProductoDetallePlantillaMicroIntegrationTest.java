package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.repository.PlantillaAnalisisMicrobiologicoRepository;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProductoDetallePlantillaMicroIntegrationTest extends IntegrationTestH2 {

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
    private PlantillaAnalisisMicrobiologicoRepository plantillaAnalisisMicrobiologicoRepository;

    @MockBean
    private com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void obtenerPorId_exponePlantillaMicroDesdeFkProducto() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("tester-micro-" + suffix)
                .clave("secret")
                .nombreCompleto("Tester Micro")
                .correo("tester-micro-" + suffix + "@example.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad Micro " + suffix)
                .simbolo("UM")
                .codigo("UM1")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria Micro " + suffix)
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        PlantillaAnalisisMicrobiologico plantilla = plantillaAnalisisMicrobiologicoRepository.save(
                PlantillaAnalisisMicrobiologico.builder()
                        .nombre("MICRO - PRODUCTO TERMINADO (PT) " + suffix)
                        .build()
        );

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("PT-" + suffix)
                .nombre("Producto Micro " + suffix)
                .stockMinimo(BigDecimal.ONE)
                .activo(true)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .plantillaAnalisisMicrobiologico(plantilla)
                .build());

        mockMvc.perform(get("/api/productos/{id}", producto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plantillaAnalisisMicroId").value(plantilla.getId()))
                .andExpect(jsonPath("$.plantillaAnalisisMicroNombre").value(containsString("PRODUCTO TERMINADO")));
    }
}
