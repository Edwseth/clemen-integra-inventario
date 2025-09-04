package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FormulaProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FormulaProductoRepository formulaRepository;
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
        formulaRepository.deleteAll();
        productoRepository.deleteAll();
        unidadMedidaRepository.deleteAll();
        categoriaProductoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void getFormulaActivaIncludesUnidadBase() throws Exception {
        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Litro").simbolo("L").build());
        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Jarabes").tipo(TipoCategoria.PRODUCTO_TERMINADO).build());
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("user").clave("pwd").nombreCompleto("User").correo("u@t.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION).activo(true).bloqueado(false).build());
        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU1").nombre("Prod").descripcionProducto("d")
                .stockActual(BigDecimal.ZERO).stockMinimo(BigDecimal.ZERO)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidad).categoriaProducto(categoria).creadoPor(usuario)
                .build());

        FormulaProducto formula = FormulaProducto.builder()
                .producto(producto)
                .estado(EstadoFormula.APROBADA)
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .creadoPor(usuario)
                .detalles(Collections.emptyList())
                .build();
        formulaRepository.save(formula);

        mockMvc.perform(get("/api/bom/formulas/activa").param("productoId", producto.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidadBaseFormula").value("L"))
                .andExpect(jsonPath("$.cantidadBaseFormula").value(1));
    }

    @Test
    @WithMockUser(authorities = {"ROL_SUPER_ADMIN"})
    void crearFormulaAceptaSimboloDeUnidad() throws Exception {
        UnidadMedida unidadProducto = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Litro").simbolo("L").build());
        UnidadMedida unidadInsumo = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Gramo").simbolo("g").build());
        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Jarabes").tipo(TipoCategoria.PRODUCTO_TERMINADO).build());
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("admin").clave("pwd").nombreCompleto("Admin").correo("a@t.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN).activo(true).bloqueado(false).build());
        Producto insumo = productoRepository.save(Producto.builder()
                .codigoSku("INS1").nombre("Insumo1").descripcionProducto("d")
                .stockActual(BigDecimal.ZERO).stockMinimo(BigDecimal.ZERO)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidadInsumo).categoriaProducto(categoria).creadoPor(usuario)
                .build());
        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("PROD1").nombre("Producto1").descripcionProducto("d")
                .stockActual(BigDecimal.ZERO).stockMinimo(BigDecimal.ZERO)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidadProducto).categoriaProducto(categoria).creadoPor(usuario)
                .build());

        String formulaJson = "{" +
                "\"productoId\":" + producto.getId() + "," +
                "\"version\":\"1.0\"," +
                "\"estado\":\"BORRADOR\"," +
                "\"creadoPorId\":" + usuario.getId() + "," +
                "\"insumos\":[{" +
                "\"productoId\":" + insumo.getId() + "," +
                "\"cantidad\":5," +
                "\"unidadMedida\":\"g\"," +
                "\"tipo\":\"OBLIGATORIO\"}]" +
                "}";

        MockMultipartFile formulaPart = new MockMultipartFile(
                "formula", "", "application/json", formulaJson.getBytes());

        mockMvc.perform(multipart("/api/bom/formulas").file(formulaPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].unidadSimbolo").value("g"));
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void jefeProduccionPuedeAprobarFormula() throws Exception {
        UnidadMedida unidadProducto = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Litro").simbolo("L").build());
        UnidadMedida unidadInsumo = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Gramo").simbolo("g").build());
        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Jarabes").tipo(TipoCategoria.PRODUCTO_TERMINADO).build());
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("prod").clave("pwd").nombreCompleto("Prod").correo("p@t.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION).activo(true).bloqueado(false).build());
        Producto insumo = productoRepository.save(Producto.builder()
                .codigoSku("INS1").nombre("Insumo1").descripcionProducto("d")
                .stockActual(BigDecimal.ZERO).stockMinimo(BigDecimal.ZERO)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidadInsumo).categoriaProducto(categoria).creadoPor(usuario)
                .build());
        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("PROD1").nombre("Producto1").descripcionProducto("d")
                .stockActual(BigDecimal.ZERO).stockMinimo(BigDecimal.ZERO)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidadProducto).categoriaProducto(categoria).creadoPor(usuario)
                .build());

        FormulaProducto formula = FormulaProducto.builder()
                .producto(producto)
                .version("1.0")
                .estado(EstadoFormula.BORRADOR)
                .fechaCreacion(LocalDateTime.now())
                .activo(false)
                .creadoPor(usuario)
                .detalles(Collections.emptyList())
                .build();
        formulaRepository.save(formula);

        String updateJson = "{" +
                "\"productoId\":" + producto.getId() + "," +
                "\"version\":\"1.0\"," +
                "\"estado\":\"APROBADA\"," +
                "\"creadoPorId\":" + usuario.getId() + "," +
                "\"insumos\":[{" +
                "\"productoId\":" + insumo.getId() + "," +
                "\"cantidad\":1," +
                "\"unidadMedida\":\"g\"," +
                "\"tipo\":\"OBLIGATORIO\"}]" +
                "}";

        mockMvc.perform(put("/api/bom/formulas/" + formula.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));
    }
}
