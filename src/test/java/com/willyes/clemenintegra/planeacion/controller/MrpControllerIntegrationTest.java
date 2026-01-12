package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionDetalle;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.repository.PlanProduccionSemanalRepository;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MrpControllerIntegrationTest extends IntegrationTestMySqlContainer {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private FormulaProductoRepository formulaProductoRepository;
    @Autowired
    private PlanProduccionSemanalRepository planProduccionSemanalRepository;
    @Autowired
    private CorridaMrpRepository corridaMrpRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    private PlanProduccionSemanal plan;
    private Producto insumo;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("comprador")
                .clave("secret")
                .nombreCompleto("Usuario Comprador")
                .correo("comprador@example.com")
                .rol(RolUsuario.ROL_COMPRADOR)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Kilogramo")
                .simbolo("KG")
                .codigo("KG")
                .build());

        CategoriaProducto categoriaPt = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("PT")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        CategoriaProducto categoriaMp = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("MP")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto productoFinal = productoRepository.save(Producto.builder()
                .codigoSku("SKU-PT-1")
                .nombre("Producto Terminado")
                .descripcionProducto("PT")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoriaPt)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .activo(true)
                .build());

        insumo = productoRepository.save(Producto.builder()
                .codigoSku("SKU-MP-1")
                .nombre("Insumo MRP")
                .descripcionProducto("MP")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoriaMp)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .activo(true)
                .build());

        FormulaProducto formula = FormulaProducto.builder()
                .producto(productoFinal)
                .version("v1")
                .estado(EstadoFormula.APROBADA)
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .creadoPor(usuario)
                .build();

        DetalleFormula detalleFormula = DetalleFormula.builder()
                .formula(formula)
                .insumo(insumo)
                .unidadMedida(unidad)
                .cantidadNecesaria(new BigDecimal("2.00"))
                .obligatorio(true)
                .build();
        formula.setDetalles(List.of(detalleFormula));
        formulaProductoRepository.save(formula);

        PlanProduccionDetalle detallePlan = PlanProduccionDetalle.builder()
                .producto(productoFinal)
                .unidadMedida(unidad)
                .cantidadPlanificada(new BigDecimal("10.00"))
                .creadoPor(usuario)
                .fechaCreacion(LocalDateTime.now())
                .build();

        plan = PlanProduccionSemanal.builder()
                .semanaInicio(LocalDate.now())
                .semanaFin(LocalDate.now().plusDays(6))
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .creadoPor(usuario)
                .build();
        detallePlan.setPlan(plan);
        plan.getDetalles().add(detallePlan);

        plan = planProduccionSemanalRepository.save(plan);
    }

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void ejecutarCorridaMrpDevuelveDetalles() throws Exception {
        mockMvc.perform(post("/api/mrp/corridas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planSemanalId\":" + plan.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].productoNombre").value(insumo.getNombre()))
                .andExpect(jsonPath("$.detalles[0].categoriaInsumo").value("MP"))
                .andExpect(jsonPath("$.sugerencias[0].productoNombre").value(insumo.getNombre()));
    }

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void ejecutarCorridaMrpNoGeneraErrorServidor() throws Exception {
        mockMvc.perform(post("/api/mrp/corridas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planSemanalId\":" + plan.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void obtenerCorridaNoGeneraStackOverflow() throws Exception {
        mockMvc.perform(post("/api/mrp/corridas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planSemanalId\":" + plan.getId() + "}"))
                .andExpect(status().isOk());

        Long corridaId = corridaMrpRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/mrp/corridas/" + corridaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(corridaId));
    }
}
