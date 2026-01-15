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
import com.willyes.clemenintegra.support.IntegrationTestH2;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class OrdenProduccionObtenerPorIdIntegrationTest extends IntegrationTestH2 {

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
    void obtenerPorIdIncluyeCategoriaProducto() throws Exception {
        Usuario usuario = usuarioRepository.findByNombreUsuario("responsable-op")
                .map(existing -> {
                    existing.setClave("secret");
                    existing.setNombreCompleto("Responsable OP");
                    existing.setCorreo("responsable-op@example.com");
                    existing.setRol(RolUsuario.ROL_JEFE_PRODUCCION);
                    existing.setActivo(true);
                    existing.setBloqueado(false);
                    return usuarioRepository.save(existing);
                })
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .nombreUsuario("responsable-op")
                        .clave("secret")
                        .nombreCompleto("Responsable OP")
                        .correo("responsable-op@example.com")
                        .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                        .activo(true)
                        .bloqueado(false)
                        .build()));

        UnidadMedida unidad = unidadMedidaRepository.findByNombreIgnoreCaseOrSimboloIgnoreCase("Unidad OP", "UOP")
                .map(existing -> {
                    existing.setSimbolo("UOP");
                    existing.setCodigo("UOP");
                    return unidadMedidaRepository.save(existing);
                })
                .orElseGet(() -> unidadMedidaRepository.save(UnidadMedida.builder()
                        .nombre("Unidad OP")
                        .simbolo("UOP")
                        .codigo("UOP")
                        .build()));

        CategoriaProducto categoria = categoriaProductoRepository.findByNombre("Categoria OP")
                .orElseGet(() -> categoriaProductoRepository.save(CategoriaProducto.builder()
                        .nombre("Categoria OP")
                        .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                        .build()));

        Producto producto = productoRepository.findByCodigoSku("SKU-OP")
                .map(existing -> {
                    existing.setNombre("Producto OP");
                    existing.setDescripcionProducto("Producto para orden");
                    existing.setStockMinimo(BigDecimal.ZERO);
                    existing.setUnidadMedida(unidad);
                    existing.setCategoriaProducto(categoria);
                    existing.setCreadoPor(usuario);
                    existing.setTipoAnalisis(TipoAnalisisCalidad.NINGUNO);
                    existing.setRequiereAnalisisFisico(false);
                    existing.setRequiereAnalisisQuimico(false);
                    existing.setRequiereAnalisisMicrobiologico(false);
                    existing.setActivo(true);
                    return productoRepository.save(existing);
                })
                .orElseGet(() -> productoRepository.save(Producto.builder()
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
                        .build()));

        OrdenProduccion orden = ordenProduccionRepository.save(OrdenProduccion.builder()
                .codigoOrden("OP-GET-001")
                .fechaInicio(LocalDateTime.now().minusHours(1))
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .estado(EstadoProduccion.EN_PROCESO)
                .producto(producto)
                .unidadMedida(unidad)
                .responsable(usuario)
                .build());

        mockMvc.perform(get("/api/produccion/ordenes/{id}", orden.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreProducto").value("Producto OP"))
                .andExpect(jsonPath("$.categoriaProducto").value(TipoCategoria.PRODUCTO_TERMINADO.name()))
                .andExpect(jsonPath("$.unidadMedida").value("UNIDAD OP"))
                .andExpect(jsonPath("$.unidadMedidaSimbolo").value("UOP"));
    }
}
