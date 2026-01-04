package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.model.ChecklistEtapaItem;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.ChecklistEtapaItemRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChecklistEtapaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrdenProduccionRepository ordenProduccionRepository;
    @Autowired
    private EtapaProduccionRepository etapaProduccionRepository;
    @Autowired
    private ChecklistEtapaItemRepository checklistEtapaItemRepository;
    @MockBean
    private com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    private Long ordenId;
    private Long etapaId;

    @BeforeEach
    void setUp() {
        OrdenProduccion orden = ordenProduccionRepository.save(OrdenProduccion.builder()
                .codigoOrden("OP-CL-1")
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(BigDecimal.ONE)
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .estado(EstadoProduccion.CREADA)
                .build());

        EtapaProduccion etapa = etapaProduccionRepository.save(EtapaProduccion.builder()
                .nombre("Preparación")
                .secuencia(1)
                .estado(EstadoEtapa.PENDIENTE)
                .ordenProduccion(orden)
                .build());

        this.ordenId = orden.getId();
        this.etapaId = etapa.getId();
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void obtenerChecklist_conItems() throws Exception {
        checklistEtapaItemRepository.save(ChecklistEtapaItem.builder()
                .etapaProduccion(etapaProduccionRepository.getReferenceById(etapaId))
                .nombrePaso("Verificar insumos")
                .obligatorio(true)
                .completado(false)
                .build());

        mockMvc.perform(get("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/checklist", ordenId, etapaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].nombrePaso").value("Verificar insumos"))
                .andExpect(jsonPath("$.completo").value(false))
                .andExpect(jsonPath("$.faltantesObligatorios").value(1));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void obtenerChecklist_sinItems() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/checklist", ordenId, etapaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.completo").value(false))
                .andExpect(jsonPath("$.faltantesObligatorios").value(0));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void completarItemChecklist_actualizaEstado() throws Exception {
        ChecklistEtapaItem item = checklistEtapaItemRepository.save(ChecklistEtapaItem.builder()
                .etapaProduccion(etapaProduccionRepository.getReferenceById(etapaId))
                .nombrePaso("Montaje equipo")
                .obligatorio(true)
                .completado(false)
                .estado(com.willyes.clemenintegra.produccion.model.enums.EstadoChecklistItem.PENDIENTE)
                .build());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/checklist/{itemId}/completar",
                        ordenId, etapaId, item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
    }
}
