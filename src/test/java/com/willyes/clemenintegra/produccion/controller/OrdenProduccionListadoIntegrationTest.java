package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.support.IntegrationTestH2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Se usa base H2 para evitar Testcontainers/Docker en este test.
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class OrdenProduccionListadoIntegrationTest extends IntegrationTestH2 {

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

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void listarOrdenesPaginadas_precargaRelaciones() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("responsable-op-" + uniqueSuffix)
                .clave("secret")
                .nombreCompleto("Responsable OP")
                .correo("resp-op-" + uniqueSuffix + "@ex.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad OP")
                .simbolo("UOP")
                .codigo("UOP")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria OP")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-OP")
                .nombre("Producto OP")
                .descripcionProducto("Producto para orden")
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

        ordenProduccionRepository.save(OrdenProduccion.builder()
                .codigoOrden("OP-001")
                .fechaInicio(LocalDateTime.now().minusHours(1))
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .estado(EstadoProduccion.EN_PROCESO)
                .producto(producto)
                .unidadMedida(unidad)
                .responsable(usuario)
                .build());

        mockMvc.perform(get("/api/produccion/ordenes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombreProducto").value("Producto OP"))
                .andExpect(jsonPath("$.content[0].nombreResponsable").value("Responsable OP"));
    }
}
