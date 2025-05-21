package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class ProductoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Long unidadId;
    private Long categoriaId;
    private Long usuarioId;

    @BeforeEach
    void setUp() {
        unidadMedidaRepository.deleteAll();
        categoriaProductoRepository.deleteAll();
        usuarioRepository.deleteAll();

        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("Kilogramo");
        unidad.setSimbolo("kg");
        unidad = unidadMedidaRepository.save(unidad);
        unidadId = unidad.getId();

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setNombre("Materia Prima");
        categoria.setTipo(TipoCategoria.MATERIA_PRIMA);
        categoria = categoriaProductoRepository.save(categoria);
        categoriaId = categoria.getId();

        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("admin");
        usuario.setNombreCompleto("Administrador");
        usuario.setCorreo("admin@erp.com");
        usuario.setRol(RolUsuario.ROL_ALMACENISTA);
        usuario.setActivo(true);
        usuario.setBloqueado(false);
        usuario.setClave(new BCryptPasswordEncoder().encode("1234"));
        usuario = usuarioRepository.save(usuario);
        usuarioId = usuario.getId();
    }

    @Test
    void crearProducto_ConDatosValidos_RetornaCreated() throws Exception {
        ProductoRequestDTO producto = new ProductoRequestDTO();
        producto.setCodigoSku("MP-001");
        producto.setNombre("Ácido Cítrico");
        producto.setDescripcionProducto("Materia prima para fórmula X");
        producto.setUnidadMedidaId(unidadId);
        producto.setCategoriaProductoId(categoriaId);
        producto.setStockActual(100);
        producto.setStockMinimo(20);
        producto.setStockMinimoProveedor(50);
        producto.setActivo(true);
        producto.setRequiereInspeccion(true);
        producto.setUsuarioId(usuarioId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoSku").value("MP-001"))
                .andExpect(jsonPath("$.nombre").value("Ácido Cítrico"));
    }

    @Test
    void crearProducto_SinUnidadMedida_RetornaBadRequest() throws Exception {
        ProductoRequestDTO producto = new ProductoRequestDTO();
        producto.setCodigoSku("MP-002");
        producto.setNombre("Magnesio P.A.");
        producto.setCategoriaProductoId(categoriaId);
        producto.setStockActual(50);
        producto.setStockMinimo(10);
        producto.setActivo(true);
        producto.setUsuarioId(usuarioId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isBadRequest());
    }
}
