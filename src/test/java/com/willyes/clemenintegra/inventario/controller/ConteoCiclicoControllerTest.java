package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoLoteResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResumenResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import com.willyes.clemenintegra.inventario.service.ConteoCiclicoService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConteoCiclicoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConteoCiclicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConteoCiclicoService conteoCiclicoService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void listarConteosDevuelve200() throws Exception {
        ConteoCiclicoResumenResponseDTO response = ConteoCiclicoResumenResponseDTO.builder()
                .id(5L)
                .almacenId(1)
                .estado(EstadoConteoCiclico.BORRADOR)
                .build();
        Page<ConteoCiclicoResumenResponseDTO> page = new PageImpl<>(List.of(response));
        when(conteoCiclicoService.listar(ArgumentMatchers.isNull(), ArgumentMatchers.isNull(), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/inventario/conteos")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5));
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void obtenerPorIdDevuelve200() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(15L)
                .almacenId(2)
                .estado(EstadoConteoCiclico.EN_CONTEO)
                .build();
        when(conteoCiclicoService.obtenerPorId(15L)).thenReturn(response);

        mockMvc.perform(get("/api/inventario/conteos/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CONTEO"));
    }


    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void listarLotesPorProductoYAlmacenDevuelveLotesEnCuarentena() throws Exception {
        ConteoCiclicoLoteResponseDTO lote = ConteoCiclicoLoteResponseDTO.builder()
                .id(501L)
                .codigoLote("L-728-01")
                .estado("EN_CUARENTENA")
                .stockLote(new BigDecimal("2.00"))
                .build();

        when(conteoCiclicoService.listarLotesParaConteo(728L, 7)).thenReturn(List.of(lote));

        mockMvc.perform(get("/api/inventario/conteos/lotes")
                        .param("productoId", "728")
                        .param("almacenId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(501))
                .andExpect(jsonPath("$[0].codigoLote").value("L-728-01"))
                .andExpect(jsonPath("$[0].estado").value("EN_CUARENTENA"));
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void listarLotesParaConteoSinQDevuelveListado() throws Exception {
        ConteoCiclicoLoteResponseDTO lote = ConteoCiclicoLoteResponseDTO.builder()
                .id(11L)
                .codigoLote("FAFAFSAF")
                .stockLote(new BigDecimal("3.00"))
                .build();
        when(conteoCiclicoService.listarLotesParaConteo(8L, 3L, null, null))
                .thenReturn(List.of(lote));

        mockMvc.perform(get("/api/inventario/conteos/8/lotes")
                        .param("productoId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].codigoLote").value("FAFAFSAF"));
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void listarLotesParaConteoConQParcialDevuelve200() throws Exception {
        ConteoCiclicoLoteResponseDTO lote = ConteoCiclicoLoteResponseDTO.builder()
                .id(9L)
                .codigoLote("L20251003-01")
                .stockLote(new BigDecimal("5.00"))
                .build();
        when(conteoCiclicoService.listarLotesParaConteo(8L, 3L, 2L, "2025"))
                .thenReturn(List.of(lote));

        mockMvc.perform(get("/api/inventario/conteos/8/lotes")
                        .param("productoId", "3")
                        .param("ubicacionFisicaId", "2")
                        .param("q", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].codigoLote").value("L20251003-01"));
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void listarLotesParaConteoSinProductoIdDevuelve400() throws Exception {
        mockMvc.perform(get("/api/inventario/conteos/8/lotes"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void obtenerPorIdNoEncontradoDevuelve404() throws Exception {
        when(conteoCiclicoService.obtenerPorId(99L))
                .thenThrow(new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "Conteo no encontrado"));

        mockMvc.perform(get("/api/inventario/conteos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.RECURSO_NO_ENCONTRADO.name()));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void crearConteoDevuelve201() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(5L)
                .almacenId(1)
                .estado(EstadoConteoCiclico.BORRADOR)
                .build();
        when(conteoCiclicoService.crearConteo(ArgumentMatchers.any(ConteoCiclicoRequestDTO.class)))
                .thenReturn(response);

        ConteoCiclicoRequestDTO request = new ConteoCiclicoRequestDTO();
        request.setAlmacenId(1);

        mockMvc.perform(post("/api/inventario/conteos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void aplicarConteoRespondeOk() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(7L)
                .almacenId(2)
                .estado(EstadoConteoCiclico.APLICADO)
                .aplicadoEn(LocalDateTime.now())
                .build();
        when(conteoCiclicoService.aplicar(anyLong(), eq("k1"))).thenReturn(response);

        mockMvc.perform(post("/api/inventario/conteos/7/aplicar")
                        .header("Idempotency-Key", "k1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APLICADO"))
                .andExpect(jsonPath("$.aplicadoEn").exists());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void iniciarConteoDevuelve200() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(3L)
                .almacenId(1)
                .estado(EstadoConteoCiclico.EN_CONTEO)
                .build();
        when(conteoCiclicoService.marcarEnConteo(3L)).thenReturn(response);

        mockMvc.perform(post("/api/inventario/conteos/3/iniciar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CONTEO"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void cerrarConteoDevuelve200() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(4L)
                .almacenId(2)
                .estado(EstadoConteoCiclico.CERRADO)
                .build();
        when(conteoCiclicoService.cerrar(4L)).thenReturn(response);

        mockMvc.perform(post("/api/inventario/conteos/4/cerrar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void transicionInvalidaDevuelve409() throws Exception {
        when(conteoCiclicoService.cerrar(21L))
                .thenThrow(new CustomBusinessException(ApiErrorCode.CONTEO_ESTADO_INVALIDO, "Transición de estado no permitida"));

        mockMvc.perform(post("/api/inventario/conteos/21/cerrar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.CONTEO_ESTADO_INVALIDO.name()));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void iniciarConteoNoEncontradoDevuelve404() throws Exception {
        when(conteoCiclicoService.marcarEnConteo(30L))
                .thenThrow(new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "Conteo no encontrado"));

        mockMvc.perform(post("/api/inventario/conteos/30/iniciar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.RECURSO_NO_ENCONTRADO.name()));
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    void listarConEstadoInvalidoDevuelve400() throws Exception {
        when(conteoCiclicoService.listar(ArgumentMatchers.isNull(), anyString(), ArgumentMatchers.any(Pageable.class)))
                .thenThrow(new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Estado de conteo inválido"));

        mockMvc.perform(get("/api/inventario/conteos")
                        .param("estado", "INVALIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.SOLICITUD_INVALIDA.name()));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void actualizarConteoDevuelve200() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(4L)
                .almacenId(3)
                .estado(EstadoConteoCiclico.BORRADOR)
                .build();
        when(conteoCiclicoService.actualizarConteo(eq(4L), ArgumentMatchers.anyList())).thenReturn(response);

        String body = objectMapper.writeValueAsString(Map.of(
                "detalles", List.of(Map.of(
                        "productoId", 11,
                        "loteProductoId", 5,
                        "conteoFisico", new BigDecimal("2.00")
                ))));

        mockMvc.perform(put("/api/inventario/conteos/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void actualizarConteoNoEncontradoDevuelve404() throws Exception {
        when(conteoCiclicoService.actualizarConteo(eq(9L), ArgumentMatchers.anyList()))
                .thenThrow(new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "Conteo no encontrado"));

        String body = objectMapper.writeValueAsString(Map.of(
                "detalles", List.of(Map.of(
                        "productoId", 9,
                        "loteProductoId", 5,
                        "conteoFisico", BigDecimal.ONE
                ))));

        mockMvc.perform(put("/api/inventario/conteos/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.RECURSO_NO_ENCONTRADO.name()));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void actualizarConteoEstadoInvalidoDevuelve409() throws Exception {
        when(conteoCiclicoService.actualizarConteo(eq(12L), ArgumentMatchers.anyList()))
                .thenThrow(new CustomBusinessException(ApiErrorCode.CONTEO_ESTADO_INVALIDO, "Estado no permite edición"));

        String body = objectMapper.writeValueAsString(Map.of(
                "detalles", List.of(Map.of(
                        "productoId", 1,
                        "loteProductoId", 5,
                        "conteoFisico", BigDecimal.ONE
                ))));

        mockMvc.perform(put("/api/inventario/conteos/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.CONTEO_ESTADO_INVALIDO.name()));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void actualizarConteoToleraCamposExtras() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(14L)
                .almacenId(1)
                .estado(EstadoConteoCiclico.BORRADOR)
                .build();
        when(conteoCiclicoService.actualizarConteo(eq(14L), ArgumentMatchers.anyList())).thenReturn(response);

        Map<String, Object> detalle = new java.util.LinkedHashMap<>();
        detalle.put("productoId", 36);
        detalle.put("loteProductoId", 77);
        detalle.put("ubicacionFisicaId", null);
        detalle.put("conteoFisico", new BigDecimal("10000"));
        detalle.put("aplicadoEn", "2024-01-01T00:00:00");

        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("fechaCreacion", "2023-12-31");
        request.put("detalles", List.of(detalle));

        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/inventario/conteos/14")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(14))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void actualizarConteoConDatoInvalidoDetallaCampo() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "detalles", List.of(Map.of(
                        "productoId", 36,
                        "loteProductoId", 5,
                        "conteoFisico", "no-numero"
                ))));

        mockMvc.perform(put("/api/inventario/conteos/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.SOLICITUD_INVALIDA.name()))
                .andExpect(jsonPath("$.message").value(containsString("detalles.[0].conteoFisico")))
                .andExpect(jsonPath("$.details.field").value("detalles.[0].conteoFisico"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void actualizarConteoSinLoteDevuelve400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "detalles", List.of(Map.of(
                        "productoId", 36,
                        "conteoFisico", BigDecimal.ONE
                ))));

        mockMvc.perform(put("/api/inventario/conteos/25")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.SOLICITUD_INVALIDA.name()))
                .andExpect(jsonPath("$.details[0].message").value("Debe seleccionar un lote"));
    }
}
