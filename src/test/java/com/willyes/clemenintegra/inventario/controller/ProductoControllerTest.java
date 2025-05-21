package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.Usuario;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    @Autowired private ProductoRepository productoRepository;

    private Long unidadId;
    private Long categoriaId;
    private Long usuarioId;

    @BeforeEach
    void setUp() {
        productoRepository.deleteAll();
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
        usuario.setClave("1234");
        usuario = usuarioRepository.save(usuario);
        usuarioId = usuario.getId();
    }

    @Test
    void crearProducto_SkuDuplicado_RetornaConflict() throws Exception {
        ProductoRequestDTO producto1 = new ProductoRequestDTO();
        producto1.setCodigoSku("MP-001");
        producto1.setNombre("Producto 1");
        producto1.setDescripcionProducto("Desc 1");
        producto1.setUnidadMedidaId(unidadId);
        producto1.setCategoriaProductoId(categoriaId);
        producto1.setStockActual(100);
        producto1.setStockMinimo(20);
        producto1.setStockMinimoProveedor(50);
        producto1.setActivo(true);
        producto1.setRequiereInspeccion(false);
        producto1.setUsuarioId(usuarioId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto1)))
                .andExpect(status().isCreated());

        ProductoRequestDTO producto2 = new ProductoRequestDTO();
        producto2.setCodigoSku("MP-001"); // duplicado
        producto2.setNombre("Producto duplicado");
        producto2.setDescripcionProducto("Otro");
        producto2.setUnidadMedidaId(unidadId);
        producto2.setCategoriaProductoId(categoriaId);
        producto2.setStockActual(30);
        producto2.setStockMinimo(5);
        producto2.setStockMinimoProveedor(10);
        producto2.setActivo(true);
        producto2.setRequiereInspeccion(false);
        producto2.setUsuarioId(usuarioId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto2)))
                .andDo(result -> System.out.println("SEGUNDO POST - BODY:" + result.getResponse().getContentAsString()))
                .andExpect(status().isConflict());
    }


    @Test
    void crearProducto_RequiereInspeccion_True_RetornaCreated() throws Exception {
        ProductoRequestDTO producto = new ProductoRequestDTO();
        producto.setCodigoSku("MP-002");
        producto.setNombre("Producto Inspeccionado");
        producto.setDescripcionProducto("Producto con calidad");
        producto.setUnidadMedidaId(unidadId);
        producto.setCategoriaProductoId(categoriaId);
        producto.setStockActual(50);
        producto.setStockMinimo(10);
        producto.setStockMinimoProveedor(20);
        producto.setActivo(true);
        producto.setRequiereInspeccion(true);
        producto.setUsuarioId(usuarioId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiereInspeccion").value(true));
    }

    @Test
    void consultarProductosPorCategoria_RetornaLista() throws Exception {
        mockMvc.perform(get("/api/productos?categoria=MATERIA_PRIMA"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
