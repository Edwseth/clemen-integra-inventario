package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.DocumentoFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.model.enums.TipoDocumento;
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
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class FormulaProductoDetalleIntegrationTest extends IntegrationTestMySqlContainer {

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

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void obtenerDetalleFormula_porId_incluyeDetalles() throws Exception {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("bom-det-user")
                .clave("secret")
                .nombreCompleto("Usuario BOM Detalle")
                .correo("bom-det-user@example.com")
                .rol(RolUsuario.ROL_JEFE_PRODUCCION)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Litro BOM")
                .simbolo("LBO")
                .codigo("LBO")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria BOM Det")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-FORM-DET")
                .nombre("Producto Formula Detalle")
                .descripcionProducto("Producto formula detalle")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .activo(true)
                .build());

        Producto insumo = productoRepository.save(Producto.builder()
                .codigoSku("SKU-INS-DET")
                .nombre("Insumo Detalle")
                .descripcionProducto("Insumo detalle")
                .stockMinimo(BigDecimal.ZERO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .requiereAnalisisFisico(false)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(false)
                .activo(true)
                .build());

        DocumentoFormula documento = DocumentoFormula.builder()
                .tipoDocumento(TipoDocumento.PROCEDIMIENTO)
                .nombreArchivo("procedimiento.pdf")
                .rutaArchivo("/tmp/procedimiento.pdf")
                .fechaSubida(LocalDateTime.now())
                .usuario(usuario)
                .build();

        FormulaProducto formula = FormulaProducto.builder()
                .producto(producto)
                .version("V2")
                .estado(EstadoFormula.EN_REVISION)
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .creadoPor(usuario)
                .detalles(List.of())
                .documentos(List.of(documento))
                .build();

        DetalleFormula detalle = DetalleFormula.builder()
                .formula(formula)
                .insumo(insumo)
                .unidadMedida(unidad)
                .cantidadNecesaria(BigDecimal.valueOf(2))
                .obligatorio(true)
                .build();

        documento.setFormula(formula);
        formula.setDetalles(List.of(detalle));
        FormulaProducto guardada = formulaProductoRepository.save(formula);

        mockMvc.perform(get("/api/bom/formulas/{id}", guardada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(guardada.getId()))
                .andExpect(jsonPath("$.productoNombre").value("Producto Formula Detalle"))
                .andExpect(jsonPath("$.detalles").isArray())
                .andExpect(jsonPath("$.detalles[0].insumoNombre").value("Insumo Detalle"))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2))
                .andExpect(jsonPath("$.documentos").isArray())
                .andExpect(jsonPath("$.documentos[0].nombreVisible").value("procedimiento.pdf"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void obtenerDetalleFormula_noExiste_retorna404() throws Exception {
        mockMvc.perform(get("/api/bom/formulas/{id}", 999999L))
                .andExpect(status().isNotFound());
    }
}
