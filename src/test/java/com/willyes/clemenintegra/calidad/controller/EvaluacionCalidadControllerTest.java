package com.willyes.clemenintegra.calidad.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class EvaluacionCalidadControllerTest {

    private static final Logger log = LoggerFactory.getLogger(EvaluacionCalidadControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoteProductoRepository loteProductoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void crearEvaluacionCalidad_debeRegistrarCorrectamente() throws Exception {
        loteProductoRepository.findAll().forEach(lp -> {
            log.info("📦 Lote: {} | Producto ID: {}", lp.getCodigoLote(), lp.getProducto().getId());
        });

        // Buscar lote por código
        LoteProducto lote = loteProductoRepository.findByCodigoLote("LOTE-PRUEBA-001")
                .orElseThrow(() -> new IllegalStateException("Lote LOTE-PRUEBA-001 no encontrado"));

        // Buscar usuario evaluador
        Usuario usuario = usuarioRepository.findByNombreUsuario("testuser")
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        // Preparar DTO
        EvaluacionCalidadRequestDTO dto = EvaluacionCalidadRequestDTO.builder()
                .loteProductoId(lote.getId())
                .usuarioEvaluadorId(usuario.getId())
                .resultado(ResultadoEvaluacion.APROBADO)
                .observaciones("Lote cumple con las condiciones de calidad")
                .archivoAdjunto(null) // Si hay archivo, se prueba en otro test
                .build();

        // Ejecutar POST y verificar respuesta
        mockMvc.perform(post("/api/calidad/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("APROBADO"))
                .andExpect(jsonPath("$.nombreLote").value(lote.getCodigoLote()))
                .andExpect(jsonPath("$.nombreEvaluador").value(usuario.getNombreCompleto()));

    }

    @Test
    void evaluarLoteYVerificarCambioEstadoALiberado() throws Exception {
        // Buscar lote en CUARENTENA
        LoteProducto lote = loteProductoRepository.findByCodigoLote("LOTE-EN_CUARENTENA-001")
                .orElseThrow(() -> new IllegalStateException("Lote no encontrado"));

        assertEquals(EstadoLote.EN_CUARENTENA, lote.getEstado());

        // Buscar usuario evaluador
        Usuario usuario = usuarioRepository.findByNombreUsuario("testuser")
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        // Crear DTO con resultado APROBADO
        EvaluacionCalidadRequestDTO dto = EvaluacionCalidadRequestDTO.builder()
                .loteProductoId(lote.getId())
                .usuarioEvaluadorId(usuario.getId())
                .resultado(ResultadoEvaluacion.APROBADO)
                .observaciones("Liberado tras control de calidad.")
                .build();

        // Ejecutar POST
        mockMvc.perform(post("/api/calidad/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("APROBADO"));

        // Verificar cambio de estado del lote
        LoteProducto loteActualizado = loteProductoRepository.findById(lote.getId())
                .orElseThrow(() -> new IllegalStateException("Lote no encontrado tras evaluación"));

        assertEquals(EstadoLote.DISPONIBLE, loteActualizado.getEstado());
    }

}
