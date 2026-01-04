package com.willyes.clemenintegra.produccion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaTemplateCopyRequest;
import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaTemplateRequest;
import com.willyes.clemenintegra.produccion.model.ChecklistEtapaTemplate;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.repository.ChecklistEtapaTemplateRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChecklistEtapaTemplateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private EtapaPlantillaRepository etapaPlantillaRepository;

    @Autowired
    private ChecklistEtapaTemplateRepository checklistEtapaTemplateRepository;

    @MockBean
    private com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    private EtapaPlantilla etapaDestino;
    private EtapaPlantilla etapaOrigen;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("jefe")
                .clave("secret")
                .nombreCompleto("Jefe Produccion")
                .correo("jefe@example.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .sessionVersion(0L)
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PT")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("KILOGRAMO")
                .simbolo("KG")
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-1")
                .nombre("Producto 1")
                .stockMinimo(BigDecimal.ZERO)
                .stockMinimoProveedor(BigDecimal.ZERO)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .build());

        etapaDestino = etapaPlantillaRepository.save(EtapaPlantilla.builder()
                .producto(producto)
                .nombre("Etapa Destino")
                .secuencia(1)
                .activo(true)
                .build());

        etapaOrigen = etapaPlantillaRepository.save(EtapaPlantilla.builder()
                .producto(producto)
                .nombre("Etapa Origen")
                .secuencia(2)
                .activo(true)
                .build());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void crearYListarChecklistTemplate() throws Exception {
        ChecklistEtapaTemplateRequest request = new ChecklistEtapaTemplateRequest();
        request.setNombreItem("Verificar insumos");
        request.setObligatorio(true);
        request.setPermitirNoAplica(false);
        request.setOrden(1);
        request.setActivo(true);

        mockMvc.perform(post("/api/produccion/plantillas-etapas/{id}/checklist", etapaDestino.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreItem").value("Verificar insumos"))
                .andExpect(jsonPath("$.etapaPlantillaId").value(etapaDestino.getId()));

        mockMvc.perform(get("/api/produccion/plantillas-etapas/{id}/checklist", etapaDestino.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreItem").value("Verificar insumos"))
                .andExpect(jsonPath("$[0].orden").value(1));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void actualizarChecklistTemplate() throws Exception {
        ChecklistEtapaTemplate existente = checklistEtapaTemplateRepository.save(ChecklistEtapaTemplate.builder()
                .etapaPlantilla(etapaDestino)
                .nombreItem("Paso previo")
                .obligatorio(true)
                .permitirNoAplica(false)
                .orden(1)
                .activo(true)
                .build());

        ChecklistEtapaTemplateRequest request = new ChecklistEtapaTemplateRequest();
        request.setNombreItem("Paso actualizado");
        request.setObligatorio(false);
        request.setPermitirNoAplica(true);
        request.setOrden(2);
        request.setActivo(true);

        mockMvc.perform(put("/api/produccion/checklist-template/{id}", existente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreItem").value("Paso actualizado"))
                .andExpect(jsonPath("$.obligatorio").value(false))
                .andExpect(jsonPath("$.permitirNoAplica").value(true))
                .andExpect(jsonPath("$.orden").value(2));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void eliminarChecklistTemplate_softDelete() throws Exception {
        ChecklistEtapaTemplate existente = checklistEtapaTemplateRepository.save(ChecklistEtapaTemplate.builder()
                .etapaPlantilla(etapaDestino)
                .nombreItem("Paso a eliminar")
                .obligatorio(true)
                .permitirNoAplica(false)
                .orden(1)
                .activo(true)
                .build());

        mockMvc.perform(delete("/api/produccion/checklist-template/{id}", existente.getId()))
                .andExpect(status().isNoContent());

        ChecklistEtapaTemplate eliminado = checklistEtapaTemplateRepository.findById(existente.getId()).orElseThrow();
        assertThat(eliminado.getActivo()).isFalse();
        assertThat(eliminado.getUpdatedAt()).isNotNull();
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void copiarChecklistTemplate_conflictoCuandoDestinoTieneChecklist() throws Exception {
        checklistEtapaTemplateRepository.save(ChecklistEtapaTemplate.builder()
                .etapaPlantilla(etapaDestino)
                .nombreItem("Paso existente")
                .obligatorio(true)
                .permitirNoAplica(false)
                .orden(1)
                .activo(true)
                .build());

        checklistEtapaTemplateRepository.save(ChecklistEtapaTemplate.builder()
                .etapaPlantilla(etapaOrigen)
                .nombreItem("Paso origen")
                .obligatorio(true)
                .permitirNoAplica(false)
                .orden(1)
                .activo(true)
                .build());

        ChecklistEtapaTemplateCopyRequest request = new ChecklistEtapaTemplateCopyRequest();
        request.setOrigenEtapaPlantillaId(etapaOrigen.getId());

        mockMvc.perform(post("/api/produccion/plantillas-etapas/{id}/checklist/copiar-desde", etapaDestino.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void copiarChecklistTemplate_exitoso() throws Exception {
        checklistEtapaTemplateRepository.save(ChecklistEtapaTemplate.builder()
                .etapaPlantilla(etapaOrigen)
                .nombreItem("Paso origen 1")
                .obligatorio(true)
                .permitirNoAplica(false)
                .orden(1)
                .activo(true)
                .build());
        checklistEtapaTemplateRepository.save(ChecklistEtapaTemplate.builder()
                .etapaPlantilla(etapaOrigen)
                .nombreItem("Definir checklist operativo")
                .obligatorio(true)
                .permitirNoAplica(false)
                .orden(2)
                .activo(true)
                .build());

        ChecklistEtapaTemplateCopyRequest request = new ChecklistEtapaTemplateCopyRequest();
        request.setOrigenEtapaPlantillaId(etapaOrigen.getId());

        mockMvc.perform(post("/api/produccion/plantillas-etapas/{id}/checklist/copiar-desde", etapaDestino.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].nombreItem").value("Paso origen 1"))
                .andExpect(jsonPath("$[0].orden").value(1))
                .andExpect(jsonPath("$[0].etapaPlantillaId").value(etapaDestino.getId()));
    }
}
