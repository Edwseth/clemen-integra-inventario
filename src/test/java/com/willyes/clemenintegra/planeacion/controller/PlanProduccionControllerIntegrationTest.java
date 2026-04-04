package com.willyes.clemenintegra.planeacion.controller;

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
import com.willyes.clemenintegra.planeacion.model.PlanProduccionDetalle;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.repository.PlanProduccionDetalleRepository;
import com.willyes.clemenintegra.planeacion.repository.PlanProduccionSemanalRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlanProduccionControllerIntegrationTest extends IntegrationTestMySqlContainer {

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
    private PlanProduccionSemanalRepository planProduccionSemanalRepository;

    @Autowired
    private PlanProduccionDetalleRepository planProduccionDetalleRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    private Usuario usuario;
    private Producto producto;

    @BeforeEach
    void setUp() {
        usuario = usuarioRepository.save(Usuario.builder()
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

        producto = productoRepository.save(Producto.builder()
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
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void crearPlanSemanalGuardaAuditoriaEnDetalle() throws Exception {
        Map<String, Object> payload = Map.of(
                "semanaInicio", "2024-10-07",
                "semanaFin", "2024-10-13",
                "detalles", List.of(
                        Map.of(
                                "productoId", producto.getId().longValue(),
                                "cantidadPlanificada", 5,
                                "unidadMedidaId", producto.getUnidadMedida().getId(),
                                "prioridad", 1,
                                "origenDemanda", "Test",
                                "observacion", "Obs"
                        )
                )
        );

        mockMvc.perform(post("/api/planeacion/planes-semanales")
                        .with(SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(usuario)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        assertThat(planProduccionSemanalRepository.findAll()).hasSize(1);
        List<PlanProduccionDetalle> detalles = planProduccionDetalleRepository.findAll();
        assertThat(detalles).hasSize(1);
        PlanProduccionDetalle detalleGuardado = detalles.getFirst();
        assertThat(detalleGuardado.getCreadoPor()).isNotNull();
        assertThat(detalleGuardado.getCreadoPor().getId()).isEqualTo(usuario.getId());
        assertThat(detalleGuardado.getFechaCreacion()).isNotNull();
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void obtenerPlanSemanalIncluyeDetalles() throws Exception {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .semanaInicio(LocalDate.now())
                .semanaFin(LocalDate.now().plusDays(6))
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .creadoPor(usuario)
                .build();

        PlanProduccionDetalle detalle = PlanProduccionDetalle.builder()
                .plan(plan)
                .producto(producto)
                .unidadMedida(producto.getUnidadMedida())
                .cantidadPlanificada(new BigDecimal("5.00"))
                .prioridad(1)
                .origenDemanda("Test")
                .observacion("Obs")
                .creadoPor(usuario)
                .fechaCreacion(LocalDateTime.now())
                .build();
        plan.getDetalles().add(detalle);

        plan = planProduccionSemanalRepository.save(plan);

        mockMvc.perform(get("/api/planeacion/planes-semanales/{id}", plan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].producto.id").value(producto.getId().longValue()))
                .andExpect(jsonPath("$.detalles[0].producto.unidadMedida").value(producto.getUnidadMedida().getSimbolo()))
                .andExpect(jsonPath("$.detalles[0].unidadMedida.id").value(producto.getUnidadMedida().getId()))
                .andExpect(jsonPath("$.detalles[0].tipoProducto").value("PT"))
                .andExpect(jsonPath("$.detalles[0].esHomeopatico").value(false))
                .andExpect(jsonPath("$.detalles[0].totalOpAsociadas").value(0))
                .andExpect(jsonPath("$.detalles[0].cantidadTotalProgramadaEnOp").value(0))
                .andExpect(jsonPath("$.detalles[0].cantidadPendiente").value(5.00))
                .andExpect(jsonPath("$.detalles[0].opIds").isArray())
                .andExpect(jsonPath("$.detalles[0].opCodigos").isArray())
                .andExpect(jsonPath("$.detalles[0].modoAccionSugerido").value("UNICA"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void confirmarPlanSemanalDevuelveDetallesYConfirma() throws Exception {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .semanaInicio(LocalDate.now())
                .semanaFin(LocalDate.now().plusDays(6))
                .estado(EstadoPlanProduccion.BORRADOR)
                .creadoPor(usuario)
                .build();

        PlanProduccionDetalle detalle = PlanProduccionDetalle.builder()
                .plan(plan)
                .producto(producto)
                .unidadMedida(producto.getUnidadMedida())
                .cantidadPlanificada(new BigDecimal("5.00"))
                .prioridad(1)
                .origenDemanda("Test")
                .observacion("Obs")
                .creadoPor(usuario)
                .fechaCreacion(LocalDateTime.now())
                .build();
        plan.getDetalles().add(detalle);

        plan = planProduccionSemanalRepository.save(plan);

        mockMvc.perform(post("/api/planeacion/planes-semanales/{id}/confirmar", plan.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(usuario))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(EstadoPlanProduccion.CONFIRMADO.name()))
                .andExpect(jsonPath("$.detalles[0].producto.id").value(producto.getId().longValue()))
                .andExpect(jsonPath("$.detalles[0].unidadMedida.id").value(producto.getUnidadMedida().getId()));

        PlanProduccionSemanal confirmado = planProduccionSemanalRepository.findById(plan.getId()).orElseThrow();
        assertThat(confirmado.getEstado()).isEqualTo(EstadoPlanProduccion.CONFIRMADO);
        assertThat(confirmado.getFechaConfirmacion()).isNotNull();
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void cerrarPlanSemanalDevuelveDetallesYCierra() throws Exception {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .semanaInicio(LocalDate.now())
                .semanaFin(LocalDate.now().plusDays(6))
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .creadoPor(usuario)
                .build();

        PlanProduccionDetalle detalle = PlanProduccionDetalle.builder()
                .plan(plan)
                .producto(producto)
                .unidadMedida(producto.getUnidadMedida())
                .cantidadPlanificada(new BigDecimal("5.00"))
                .prioridad(1)
                .origenDemanda("Test")
                .observacion("Obs")
                .creadoPor(usuario)
                .fechaCreacion(LocalDateTime.now())
                .build();
        plan.getDetalles().add(detalle);

        plan = planProduccionSemanalRepository.save(plan);

        mockMvc.perform(post("/api/planeacion/planes-semanales/{id}/cerrar", plan.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(usuario))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(EstadoPlanProduccion.CERRADO.name()))
                .andExpect(jsonPath("$.detalles[0].producto.id").value(producto.getId().longValue()))
                .andExpect(jsonPath("$.detalles[0].unidadMedida.id").value(producto.getUnidadMedida().getId()));

        PlanProduccionSemanal cerrado = planProduccionSemanalRepository.findById(plan.getId()).orElseThrow();
        assertThat(cerrado.getEstado()).isEqualTo(EstadoPlanProduccion.CERRADO);
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void listarPlanesIncluyeFechaConfirmacion() throws Exception {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .semanaInicio(LocalDate.now())
                .semanaFin(LocalDate.now().plusDays(6))
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .creadoPor(usuario)
                .fechaConfirmacion(LocalDateTime.now().withNano(0))
                .build();
        planProduccionSemanalRepository.save(plan);

        mockMvc.perform(get("/api/planeacion/planes-semanales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fechaConfirmacion").isNotEmpty());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void crearPlanDerivaUnidadDesdeProductoCuandoNoLlegaEnRequest() throws Exception {
        Map<String, Object> payload = Map.of(
                "semanaInicio", "2024-11-04",
                "semanaFin", "2024-11-10",
                "detalles", List.of(
                        Map.of(
                                "productoId", producto.getId().longValue(),
                                "cantidadPlanificada", 12,
                                "prioridad", 1,
                                "origenDemanda", "Test",
                                "observacion", "Sin unidad en request"
                        )
                )
        );

        mockMvc.perform(post("/api/planeacion/planes-semanales")
                        .with(SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(usuario)))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].unidadMedidaId").value(producto.getUnidadMedida().getId()))
                .andExpect(jsonPath("$.detalles[0].unidadMedida.id").value(producto.getUnidadMedida().getId()))
                .andExpect(jsonPath("$.detalles[0].producto.unidadMedida").value(producto.getUnidadMedida().getSimbolo()));

        List<PlanProduccionDetalle> detalles = planProduccionDetalleRepository.findAll();
        assertThat(detalles).hasSize(1);
        assertThat(detalles.getFirst().getUnidadMedida()).isNotNull();
        assertThat(detalles.getFirst().getUnidadMedida().getId()).isEqualTo(producto.getUnidadMedida().getId());
    }
}
