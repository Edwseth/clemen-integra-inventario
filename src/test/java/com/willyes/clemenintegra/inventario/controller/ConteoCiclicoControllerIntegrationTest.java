package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.ConteoCiclico;
import com.willyes.clemenintegra.inventario.model.ConteoCiclicoDetalle;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ConteoCiclicoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
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
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class ConteoCiclicoControllerIntegrationTest extends IntegrationTestMySqlContainer {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AlmacenRepository almacenRepository;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ConteoCiclicoRepository conteoCiclicoRepository;

    @MockBean
    private com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    private ConteoCiclico conteo;
    private Producto producto;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("contador")
                .clave("secret")
                .nombreCompleto("Usuario Contador")
                .correo("contador@example.com")
                .rol(RolUsuario.ROL_CONTADOR)
                .activo(true)
                .bloqueado(false)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Almacen Conteo")
                .ubicacion("Zona Conteo")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .simbolo("UND")
                .codigo("UND")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria Conteo")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-CONTEO-1")
                .nombre("Producto Conteo")
                .descripcionProducto("Producto para conteo")
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

        ConteoCiclicoDetalle detalle = ConteoCiclicoDetalle.builder()
                .producto(producto)
                .stockSistema(new BigDecimal("5.00"))
                .conteoFisico(new BigDecimal("7.00"))
                .diferencia(new BigDecimal("2.00"))
                .build();

        conteo = ConteoCiclico.builder()
                .almacen(almacen)
                .creadoPor(usuario)
                .estado(EstadoConteoCiclico.BORRADOR)
                .detalles(new ArrayList<>(List.of(detalle)))
                .build();
        detalle.setConteo(conteo);

        conteo = conteoCiclicoRepository.save(conteo);
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void listarConteosConDetallesDevuelve200() throws Exception {
        mockMvc.perform(get("/api/inventario/conteos")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(conteo.getId()))
                .andExpect(jsonPath("$.content[0].creadoPorNombre").value("Usuario Contador"))
                .andExpect(jsonPath("$.content[0].detalles").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void obtenerPorIdConDetallesDevuelve200() throws Exception {
        mockMvc.perform(get("/api/inventario/conteos/{id}", conteo.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creadoPorNombre").value("Usuario Contador"))
                .andExpect(jsonPath("$.detalles[0].productoId").value(producto.getId()));
    }
}
