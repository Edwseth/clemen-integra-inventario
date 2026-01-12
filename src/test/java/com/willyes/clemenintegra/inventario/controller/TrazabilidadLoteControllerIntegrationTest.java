package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.calidad.dto.EstadoCalidadLoteResponseDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrazabilidadLoteControllerIntegrationTest extends IntegrationTestMySqlContainer {

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
    private AlmacenRepository almacenRepository;

    @Autowired
    private LoteProductoRepository loteProductoRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private LoteProductoService loteProductoService;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void retornaAuditoriaLoteParaAliasInventarios() throws Exception {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("auditor")
                .clave("secret")
                .nombreCompleto("Auditor Calidad")
                .correo("auditor@example.com")
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .simbolo("U")
                .codigo("U")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria Auditoria")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-AUDIT")
                .nombre("Producto Auditoria")
                .descripcionProducto("Producto prueba auditoria")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .activo(true)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Almacen Auditoria")
                .ubicacion("Zona A")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        LoteProducto lote = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-AUDIT")
                .producto(producto)
                .almacen(almacen)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("10.00"))
                .stockReservado(BigDecimal.ZERO.setScale(6))
                .agotado(false)
                .build());

        when(loteProductoService.obtenerEstadoCalidad(lote.getId()))
                .thenReturn(EstadoCalidadLoteResponseDTO.builder()
                        .loteId(lote.getId())
                        .estadoLote("DISPONIBLE")
                        .build());

        mockMvc.perform(get("/api/inventarios/trazabilidad/lote/{loteId}", lote.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loteId").value(lote.getId()))
                .andExpect(jsonPath("$.codigoLote").value("LOTE-AUDIT"))
                .andExpect(jsonPath("$.nombreProducto").value("Producto Auditoria"));
    }
}
