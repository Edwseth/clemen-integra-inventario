package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.produccion.model.CierreProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrdenProduccionCierresControllerTest extends IntegrationTestMySqlContainer {

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
    private OrdenProduccionRepository ordenProduccionRepository;

    @Autowired
    private CierreProduccionRepository cierreProduccionRepository;

    @MockBean
    private com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setup() {
        cierreProduccionRepository.deleteAll();
        ordenProduccionRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaProductoRepository.deleteAll();
        unidadMedidaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void listarCierresIncluyeUnidadMedida() throws Exception {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("tester-cierre")
                .clave("clave")
                .nombreCompleto("Usuario Cierre")
                .correo("tester+cierre@example.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidadMedida = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .nombrePlural("Unidades")
                .simbolo("u")
                .codigo("UND")
                .simboloImpresion("u")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PT")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-CIERRE")
                .nombre("Producto Cierre")
                .stockMinimo(BigDecimal.ZERO)
                .activo(true)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(unidadMedida)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .build());

        OrdenProduccion ordenProduccion = ordenProduccionRepository.save(OrdenProduccion.builder()
                .codigoOrden("OP-400")
                .loteProduccion("LOTE-OP-400")
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(new BigDecimal("40"))
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .estado(EstadoProduccion.EN_PROCESO)
                .producto(producto)
                .unidadMedida(unidadMedida)
                .responsable(usuario)
                .build());

        cierreProduccionRepository.save(CierreProduccion.builder()
                .ordenProduccion(ordenProduccion)
                .cantidad(new BigDecimal("5"))
                .tipo(TipoCierre.PARCIAL)
                .usuarioId(usuario.getId())
                .usuarioNombre(usuario.getNombreCompleto())
                .build());

        mockMvc.perform(get("/api/produccion/ordenes/{id}/cierres", ordenProduccion.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].unidadMedidaSimbolo").value("u"));
    }
}
