package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private JwtTokenService jwtTokenService;

    private Long unidadId;
    private Long categoriaId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
        unidadMedidaRepository.deleteAll();
        categoriaProductoRepository.deleteAll();

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Kilogramo")
                .simbolo("kg")
                .build());
        unidadId = unidad.getId();

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Materia Prima")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());
        categoriaId = categoria.getId();

        usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("jefe")
                .nombreCompleto("Jefe Almacenes")
                .correo("jefe@erp.com")
                .rol(RolUsuario.ROL_JEFE_ALMACENES)
                .clave("1234")
                .activo(true)
                .bloqueado(false)
                .build());

        String token = jwtTokenService.generarToken(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        usuario.getNombreUsuario(),
                        token,
                        List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
                )
        );
    }

    @Test
    void crearProductoValido_debeRetornarCreated() throws Exception {
        ProductoRequestDTO dto = ProductoRequestDTO.builder()
                .codigoSku("SKU-NEW-001")
                .nombre("Producto Nuevo")
                .descripcionProducto("Prueba")
                .stockMinimo(BigDecimal.TEN)
                .stockMinimoProveedor(BigDecimal.valueOf(20))
                .requiereInspeccion(false)
                .unidadMedidaId(unidadId)
                .categoriaProductoId(categoriaId)
                .build();

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoSku").value("SKU-NEW-001"));
    }

    @Test
    void crearProductoInvalido_debeRetornarBadRequest() throws Exception {
        ProductoRequestDTO dto = ProductoRequestDTO.builder()
                .codigoSku("SKU-ERR-001")
                .nombre("")
                .stockMinimo(BigDecimal.valueOf(-5))
                .unidadMedidaId(unidadId)
                .categoriaProductoId(categoriaId)
                .build();

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerProductoPorId_debeRetornarProducto() throws Exception {
        UnidadMedida unidad = unidadMedidaRepository.findById(unidadId).orElseThrow();
        CategoriaProducto categoria = categoriaProductoRepository.findById(categoriaId).orElseThrow();

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-EXIST-001")
                .nombre("Existente")
                .descripcionProducto("desc")
                .stockMinimo(BigDecimal.ONE)
                .stockMinimoProveedor(BigDecimal.TEN)
                .activo(true)
                .requiereInspeccion(false)
                .fechaCreacion(LocalDateTime.now())
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .build());

        mockMvc.perform(get("/api/productos/{id}", producto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(producto.getId()))
                .andExpect(jsonPath("$.nombre").value("Existente"));
    }

    @Test
    void obtenerProductoNoExistente_debeRetornarNotFound() throws Exception {
        mockMvc.perform(get("/api/productos/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarTodosLosProductos_debeRetornarLista() throws Exception {
        UnidadMedida unidad = unidadMedidaRepository.findById(unidadId).orElseThrow();
        CategoriaProducto categoria = categoriaProductoRepository.findById(categoriaId).orElseThrow();

        productoRepository.save(Producto.builder()
                .codigoSku("SKU-LIST-001")
                .nombre("Listado")
                .descripcionProducto("desc")
                .stockMinimo(BigDecimal.ONE)
                .stockMinimoProveedor(BigDecimal.TEN)
                .activo(true)
                .requiereInspeccion(false)
                .fechaCreacion(LocalDateTime.now())
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .build());

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }
}
