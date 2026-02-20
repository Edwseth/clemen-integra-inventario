package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.servlet.multipart.max-file-size=1MB",
        "spring.servlet.multipart.max-request-size=2MB"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluacionCalidadControllerIntegrationTest extends IntegrationTestMySqlContainer {

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
    private AlmacenRepository almacenRepository;
    @Autowired
    private LoteProductoRepository loteProductoRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;

    private LoteProducto loteProducto;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("microbio.test")
                .clave("secret")
                .nombreCompleto("Microbiologo Test")
                .correo("microbio.test@example.com")
                .rol(RolUsuario.ROL_MICROBIOLOGO)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad Calidad Post")
                .simbolo("UCP")
                .codigo("UCP")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria Calidad Post")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-CAL-POST")
                .nombre("Producto Calidad Post")
                .descripcionProducto("Producto calidad post")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(true)
                .requiereAnalisisMicrobiologico(true)
                .activo(true)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Almacen Calidad Post")
                .ubicacion("Zona QA")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        loteProducto = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LP-CAL-POST")
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .stockLote(BigDecimal.TEN)
                .estado(EstadoLote.EN_CUARENTENA)
                .producto(producto)
                .almacen(almacen)
                .usuarioLiberador(usuario)
                .build());

        when(inventoryCatalogResolver.getAlmacenCuarentenaId()).thenReturn(almacen.getId().longValue());
    }


    @Test
    @WithMockUser(username = "microbio.test", authorities = "ROL_MICROBIOLOGO")
    void crearEvaluacionConArchivoMayorAlLimiteRetorna413() throws Exception {
        byte[] contenido = new byte[2 * 1024 * 1024];
        MockMultipartFile archivoGrande = new MockMultipartFile(
                "archivos",
                "informe.pdf",
                "application/pdf",
                contenido);

        mockMvc.perform(multipart("/api/calidad/evaluaciones")
                        .file(archivoGrande)
                        .param("tipoEvaluacion", "QUIMICO_MICROBIOLOGICO")
                        .param("observaciones", "Archivo grande")
                        .param("loteProductoId", loteProducto.getId().toString())
                        .param("resultado", "CONFORME"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("ARCHIVO_DEMASIADO_GRANDE"))
                .andExpect(jsonPath("$.message").value("El archivo adjunto supera el tamaño permitido."))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    @WithMockUser(username = "microbio.test", authorities = "ROL_MICROBIOLOGO")
    void crearEvaluacionNoDisparaLazyInitialization() throws Exception {
        mockMvc.perform(multipart("/api/calidad/evaluaciones")
                        .param("tipoEvaluacion", "QUIMICO_MICROBIOLOGICO")
                        .param("observaciones", "OK")
                        .param("loteProductoId", loteProducto.getId().toString())
                        .param("resultado", "CONFORME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreProducto").value("Producto Calidad Post"))
                .andExpect(jsonPath("$.nombreLote").value("LP-CAL-POST"));
    }
}
