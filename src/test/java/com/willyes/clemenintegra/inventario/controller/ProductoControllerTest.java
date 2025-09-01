package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setup() {
        productoRepository.deleteAll();
        unidadMedidaRepository.deleteAll();
        categoriaProductoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_CALIDAD"})
    void getProductoIncludesRendimientoAndUnidadMedida() throws Exception {
        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Litro").simbolo("L").build());
        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Jarabes").tipo(TipoCategoria.PRODUCTO_TERMINADO).build());
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("user").clave("pwd").nombreCompleto("User Test").correo("u@t.com")
                .rol(RolUsuario.ROL_JEFE_CALIDAD).activo(true).bloqueado(false).build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU1")
                .nombre("Producto1")
                .descripcionProducto("desc")
                .stockActual(BigDecimal.ZERO)
                .stockMinimo(BigDecimal.ZERO)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .rendimientoUnidad(new BigDecimal("4"))
                .build());

        mockMvc.perform(get("/api/productos/" + producto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendimiento").value(4))
                .andExpect(jsonPath("$.unidadMedida.simbolo").value("L"));
    }
}
