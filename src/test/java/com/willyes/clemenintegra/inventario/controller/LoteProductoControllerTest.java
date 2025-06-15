package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class LoteProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoteProductoRepository loteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private AlmacenRepository almacenRepository;

    @Test
    void crearLote_ConDatosValidos_RetornaCreated() throws Exception {
        LoteProductoRequestDTO request = LoteProductoRequestDTO.builder()
                .codigoLote("LOTE-INT-001")
                .fechaFabricacion(LocalDate.now().minusDays(3))
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .stockLote(new BigDecimal("100.5"))
                .estado(EstadoLote.DISPONIBLE)
                .temperaturaAlmacenamiento(22.0)
                .productoId(1L)
                .almacenId(1L)
                .build();

        mockMvc.perform(post("/api/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoLote").value("LOTE-INT-001"));

    }

    @Test
    void crearLote_CodigoLoteDuplicado_RetornaConflict() throws Exception {
        LoteProductoRequestDTO dto = LoteProductoRequestDTO.builder()
                .codigoLote("LOTE-DUPLICADO")
                .fechaFabricacion(LocalDate.of(2025, 5, 20))
                .fechaVencimiento(LocalDate.of(2026, 5, 23))
                .stockLote(new BigDecimal("100.00"))
                .estado(EstadoLote.DISPONIBLE)
                .productoId(1L)
                .almacenId(1L)
                .build();

        mockMvc.perform(post("/api/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Ya existe un lote con el código")));
    }

    @Test
    void crearLote_FechaVencimientoAnterior_RetornaBadRequest() throws Exception {
        LoteProductoRequestDTO dto = LoteProductoRequestDTO.builder()
                .codigoLote("LOTE-INV-001")
                .fechaFabricacion(LocalDate.now())
                .fechaVencimiento(LocalDate.now().minusDays(1))
                .stockLote(BigDecimal.TEN)
                .estado(EstadoLote.DISPONIBLE)
                .productoId(1L)
                .almacenId(1L)
                .build();

        mockMvc.perform(post("/api/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerLotePorId_Existente_RetornaOk() throws Exception {
        Producto producto = productoRepository.findById(1L).orElseThrow();
        Almacen almacen = almacenRepository.findById(1L).orElseThrow();

        LoteProducto lote = loteRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-GET-001")
                .fechaFabricacion(LocalDate.now().minusDays(2))
                .fechaVencimiento(LocalDate.now().plusDays(10))
                .stockLote(new BigDecimal("30.00"))
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacen)
                .build());

        mockMvc.perform(get("/api/lotes/{id}", lote.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lote.getId()));
    }

    @Test
    void obtenerLotePorId_NoExistente_RetornaNotFound() throws Exception {
        mockMvc.perform(get("/api/lotes/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarTodosLosLotes_RetornaLista() throws Exception {
        Producto producto = productoRepository.findById(1L).orElseThrow();
        Almacen almacen = almacenRepository.findById(1L).orElseThrow();

        loteRepository.save(LoteProducto.builder()
                .codigoLote("LOTE-LIST-001")
                .fechaFabricacion(LocalDate.now().minusDays(1))
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .stockLote(BigDecimal.ONE)
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacen)
                .build());

        mockMvc.perform(get("/api/lotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    @WithMockUser(username = "jefeCalidad", roles = {"ROL_JEFE_CALIDAD"})
    void exportarLotesPorVencer_UsuarioAutorizado_RetornaExcel() throws Exception {
        mockMvc.perform(get("/api/lotes/reporte-vencimiento"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment;")))
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @WithMockUser(username = "almacenista", roles = {"ROL_ALMACENISTA"})
    void exportarLotesPorVencer_UsuarioNoAutorizado_RetornaForbidden() throws Exception {
        mockMvc.perform(get("/api/lotes/reporte-vencimiento"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "jefeCalidad", roles = {"ROL_JEFE_CALIDAD"})
    void exportarAlertasActivas_UsuarioAutorizado_RetornaExcel() throws Exception {
        mockMvc.perform(get("/api/lotes/reporte-alertas"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment;")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @WithMockUser(username = "visitante", roles = {"ROL_ALMACENISTA"})
    void exportarAlertasActivas_UsuarioNoAutorizado_RetornaForbidden() throws Exception {
        mockMvc.perform(get("/api/lotes/reporte-alertas"))
                .andExpect(status().isForbidden());
    }


}

