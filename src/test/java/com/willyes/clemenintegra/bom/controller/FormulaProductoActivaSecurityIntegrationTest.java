package com.willyes.clemenintegra.bom.controller;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FormulaProductoActivaSecurityIntegrationTest extends IntegrationTestMySqlContainer {

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
    void obtenerFormulaActiva_permiteRolPlaneador() throws Exception {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("planeador-user")
                .clave("secret")
                .nombreCompleto("Usuario Planeador")
                .correo("planeador-user@example.com")
                .rol(RolUsuario.ROL_PLANEADOR)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Kilogramo Planeador")
                .simbolo("KPL")
                .codigo("KPL")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria Planeador")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-FORM-PLAN")
                .nombre("Producto Formula Planeador")
                .descripcionProducto("Producto formula planeador")
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
                .codigoSku("SKU-INS-PLAN")
                .nombre("Insumo Formula Planeador")
                .descripcionProducto("Insumo planeador")
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

        FormulaProducto formula = FormulaProducto.builder()
                .producto(producto)
                .version("V1")
                .estado(EstadoFormula.APROBADA)
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .creadoPor(usuario)
                .detalles(List.of())
                .documentos(List.of())
                .build();

        DetalleFormula detalle = DetalleFormula.builder()
                .formula(formula)
                .insumo(insumo)
                .unidadMedida(unidad)
                .cantidadNecesaria(BigDecimal.ONE)
                .obligatorio(true)
                .build();

        formula.setDetalles(List.of(detalle));
        formulaProductoRepository.save(formula);

        mockMvc.perform(get("/api/bom/formulas/activa")
                        .with(user("planeador").authorities(new SimpleGrantedAuthority("ROL_PLANEADOR")))
                        .param("productoId", formula.getProducto().getId().toString()))
                .andExpect(status().isOk());
    }
}
