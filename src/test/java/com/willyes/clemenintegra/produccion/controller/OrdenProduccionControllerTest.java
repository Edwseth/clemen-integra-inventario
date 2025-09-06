package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.repository.*;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
class OrdenProduccionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrdenProduccionRepository ordenRepository;
    @Autowired
    private EtapaProduccionRepository etapaProduccionRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired
    private AlmacenRepository almacenRepository;
    @Autowired
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;

    private Usuario responsable1;
    private Usuario responsable2;

    @BeforeEach
    void setup() {
        etapaProduccionRepository.deleteAll();
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
                .cantidadProgramada(BigDecimal.valueOf(10))
                .estado(EstadoProduccion.CREADA)
                .responsable(responsable1)
                .build());

        ordenRepository.save(OrdenProduccion.builder()
                .codigoOrden("ORD-B2")
                .fechaInicio(LocalDateTime.of(2023,2,1,0,0))
                .cantidadProgramada(BigDecimal.valueOf(20))
                .estado(EstadoProduccion.EN_PROCESO)
                .responsable(responsable2)
                .build());

        ordenRepository.save(OrdenProduccion.builder()
                .codigoOrden("X-ORD")
                .fechaInicio(LocalDateTime.of(2023,3,1,0,0))
                .cantidadProgramada(BigDecimal.valueOf(30))
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

        // Pre-Bodega
        almacenRepository.findByNombre("Pre-Bodega").orElseGet(() ->
                almacenRepository.save(Almacen.builder()
                        .nombre("Pre-Bodega")
                        .ubicacion("PB")
                        .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                        .tipo(TipoAlmacen.PRINCIPAL)
                        .build()));

        tipoMovimientoDetalleRepository.findByDescripcion("ENTRADA_PARCIAL_PRODUCCION")
                .orElseGet(() -> tipoMovimientoDetalleRepository.save(TipoMovimientoDetalle.builder()
                        .descripcion("ENTRADA_PARCIAL_PRODUCCION")
                        .build()));

        return ordenRepository.save(OrdenProduccion.builder()
                .codigoOrden("ORD-FIN")
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(BigDecimal.valueOf(programada))
                .estado(EstadoProduccion.EN_PROCESO)
                .producto(producto)
                .unidadMedida(producto.getUnidadMedida())
                .responsable(responsable1)
                .build());
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void obtenerOrdenIncluyeCategoriaProducto() throws Exception {
        OrdenProduccion op = crearOrdenConProducto(10);
        mockMvc.perform(get("/api/produccion/ordenes/" + op.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaProducto").value("PRODUCTO_TERMINADO"));
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

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void cierreParcialOk() throws Exception {
        OrdenProduccion op = crearOrdenConProducto(10);
        mockMvc.perform(post("/api/produccion/ordenes/" + op.getId() + "/cierres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":4,\"tipo\":\"PARCIAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadProducidaAcumulada").value(4))
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void cierreTotalCompletoOk() throws Exception {
        OrdenProduccion op = crearOrdenConProducto(10);
        mockMvc.perform(post("/api/produccion/ordenes/" + op.getId() + "/cierres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":10,\"tipo\":\"TOTAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.cantidadProducidaAcumulada").value(10));
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void cierreTotalIncompletoOk() throws Exception {
        OrdenProduccion op = crearOrdenConProducto(10);
        mockMvc.perform(post("/api/produccion/ordenes/" + op.getId() + "/cierres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":8,\"tipo\":\"TOTAL\",\"cerradaIncompleta\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA_INCOMPLETA"))
                .andExpect(jsonPath("$.cantidadProducidaAcumulada").value(8));
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void cierreExceso() throws Exception {
        OrdenProduccion op = crearOrdenConProducto(10);
        mockMvc.perform(post("/api/produccion/ordenes/" + op.getId() + "/cierres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":11,\"tipo\":\"PARCIAL\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void cierreEstadoNoValido() throws Exception {
        OrdenProduccion op = crearOrdenConProducto(10);
        // cerrar total para finalizar
        mockMvc.perform(post("/api/produccion/ordenes/" + op.getId() + "/cierres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":10,\"tipo\":\"TOTAL\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/produccion/ordenes/" + op.getId() + "/cierres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":1,\"tipo\":\"PARCIAL\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = {"ROL_JEFE_PRODUCCION"})
    void cierreConflictoVersion() throws Exception {
        OrdenProduccion op = crearOrdenConProducto(10);
        AtomicInteger conflicts = new AtomicInteger();

        Runnable task = () -> {
            try {
                var res = mockMvc.perform(post("/api/produccion/ordenes/" + op.getId() + "/cierres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cantidad\":5,\"tipo\":\"PARCIAL\"}"))
                        .andReturn();
                if (res.getResponse().getStatus() == 409) {
                    conflicts.incrementAndGet();
                }
            } catch (Exception ignored) {
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start(); t2.start();
        t1.join(); t2.join();

        assertEquals(1, conflicts.get());
    }

    @Test
    @WithUserDetails("juan")
    void iniciarPrimeraEtapaActualizaEstadoOrden() throws Exception {
        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("unidad")
                .simbolo("u")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("cat")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-INIT")
                .nombre("ProdInit")
                .descripcionProducto("desc")
                .stockActual(BigDecimal.ZERO)
                .stockMinimo(BigDecimal.ZERO)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(responsable1)
                .build());

        OrdenProduccion orden = ordenRepository.save(OrdenProduccion.builder()
                .codigoOrden("ORD-INIT")
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(BigDecimal.ONE)
                .estado(EstadoProduccion.CREADA)
                .producto(producto)
                .unidadMedida(unidad)
                .responsable(responsable1)
                .build());

        EtapaProduccion etapa = etapaProduccionRepository.save(EtapaProduccion.builder()
                .nombre("Etapa1")
                .secuencia(1)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .build());

        mockMvc.perform(patch("/api/produccion/ordenes/" + orden.getId() + "/etapas/" + etapa.getId() + "/iniciar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));

        mockMvc.perform(get("/api/produccion/ordenes/" + orden.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));
    }
}

