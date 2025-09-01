package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.repository.*;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrdenProduccionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrdenProduccionRepository ordenRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    private Usuario responsable1;
    private Usuario responsable2;

    @BeforeEach
    void setup() {
        ordenRepository.deleteAll();
        usuarioRepository.deleteAll();

        responsable1 = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("juan")
                .clave("pwd")
                .nombreCompleto("Juan Perez")
                .correo("juan@ex.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        responsable2 = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("maria")
                .clave("pwd")
                .nombreCompleto("Maria Gomez")
                .correo("maria@ex.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        ordenRepository.save(OrdenProduccion.builder()
                .codigoOrden("ORD-A1")
                .fechaInicio(LocalDateTime.of(2023,1,1,0,0))
                .cantidadProgramada(10)
                .estado(EstadoProduccion.CREADA)
                .responsable(responsable1)
                .build());

        ordenRepository.save(OrdenProduccion.builder()
                .codigoOrden("ORD-B2")
                .fechaInicio(LocalDateTime.of(2023,2,1,0,0))
                .cantidadProgramada(20)
                .estado(EstadoProduccion.EN_PROCESO)
                .responsable(responsable2)
                .build());

        ordenRepository.save(OrdenProduccion.builder()
                .codigoOrden("X-ORD")
                .fechaInicio(LocalDateTime.of(2023,3,1,0,0))
                .cantidadProgramada(30)
                .estado(EstadoProduccion.CREADA)
                .responsable(responsable1)
                .build());
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void listarSinFiltros() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes").param("page","0").param("size","10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void filtrarPorEstado() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes").param("estado","CREADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void filtrarPorRangoFechas() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes")
                .param("fechaInicio","2023-02-01T00:00:00")
                .param("fechaFin","2023-03-31T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void filtrosCombinados() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes")
                .param("codigo","A1")
                .param("responsable","juan")
                .param("estado","CREADA")
                .param("fechaInicio","2023-01-01T00:00:00")
                .param("fechaFin","2023-01-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].codigoOrden").value("ORD-A1"));
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void ordenarAscendente() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes").param("sort","fechaInicio,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigoOrden").value("ORD-A1"));
    }

    private OrdenProduccion crearOrdenConProducto(int programada) {
        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("unidad")
                .simbolo("u")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("cat")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-1")
                .nombre("Prod")
                .descripcionProducto("desc")
                .stockActual(BigDecimal.ZERO)
                .stockMinimo(BigDecimal.ZERO)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(responsable1)
                .build());

        return ordenRepository.save(OrdenProduccion.builder()
                .codigoOrden("ORD-FIN")
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(programada)
                .estado(EstadoProduccion.EN_PROCESO)
                .producto(producto)
                .responsable(responsable1)
                .build());
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void finalizarOrden() throws Exception {
        OrdenProduccion op = crearOrdenConProducto(10);
        mockMvc.perform(put("/api/produccion/ordenes/" + op.getId() + "/finalizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadProducida\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.cantidadProducida").value(5));
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void finalizarSinCantidad() throws Exception {
        OrdenProduccion op = crearOrdenConProducto(10);
        mockMvc.perform(put("/api/produccion/ordenes/" + op.getId() + "/finalizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void finalizarCantidadMayorProgramada() throws Exception {
        OrdenProduccion op = crearOrdenConProducto(10);
        mockMvc.perform(put("/api/produccion/ordenes/" + op.getId() + "/finalizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadProducida\":20}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void finalizarOrdenInexistente() throws Exception {
        mockMvc.perform(put("/api/produccion/ordenes/9999/finalizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadProducida\":1}"))
                .andExpect(status().isNotFound());
    }
}

