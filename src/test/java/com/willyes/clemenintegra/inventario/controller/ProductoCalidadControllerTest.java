package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.ProductoCalidadUpdateDTO;
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
import com.willyes.clemenintegra.support.IntegrationTestH2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductoCalidadControllerTest extends IntegrationTestH2 {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Long productoId;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder()
                .nombreUsuario("calidad.test")
                .clave("secret")
                .nombreCompleto("Jefe Calidad Test")
                .correo("calidad.test@example.com")
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .activo(true)
                .bloqueado(false)
                .build();
        usuarioRepository.save(usuario);

        UnidadMedida unidad = UnidadMedida.builder()
                .nombre("KILOGRAMO")
                .simbolo("KG")
                .build();
        unidadMedidaRepository.save(unidad);

        CategoriaProducto categoria = CategoriaProducto.builder()
                .nombre("Categoria Calidad")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build();
        categoriaProductoRepository.save(categoria);

        Producto producto = Producto.builder()
                .codigoSku("SKU-CALIDAD-001")
                .nombre("Producto Calidad")
                .stockMinimo(BigDecimal.ONE)
                .stockMinimoProveedor(BigDecimal.ZERO)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .build();
        productoRepository.save(producto);
        productoId = producto.getId().longValue();
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void jefeCalidadPuedeActualizarCamposCalidad() throws Exception {
        ProductoCalidadUpdateDTO dto = new ProductoCalidadUpdateDTO(true, false, true);

        mockMvc.perform(patch("/api/inventario/productos/{id}/calidad", productoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiereAnalisisFisico").value(true))
                .andExpect(jsonPath("$.requiereAnalisisQuimico").value(false))
                .andExpect(jsonPath("$.requiereAnalisisMicrobiologico").value(true));

        Producto actualizado = productoRepository.findById(productoId).orElseThrow();
        assertThat(actualizado.isRequiereAnalisisFisico()).isTrue();
        assertThat(actualizado.isRequiereAnalisisQuimico()).isFalse();
        assertThat(actualizado.isRequiereAnalisisMicrobiologico()).isTrue();
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void rolNoAutorizadoRecibeForbidden() throws Exception {
        ProductoCalidadUpdateDTO dto = new ProductoCalidadUpdateDTO(true, true, true);

        mockMvc.perform(patch("/api/inventario/productos/{id}/calidad", productoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void idInexistenteDevuelveNotFound() throws Exception {
        ProductoCalidadUpdateDTO dto = new ProductoCalidadUpdateDTO(true, true, false);

        mockMvc.perform(patch("/api/inventario/productos/{id}/calidad", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }
}
