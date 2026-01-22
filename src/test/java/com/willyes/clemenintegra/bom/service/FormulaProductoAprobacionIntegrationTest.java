package com.willyes.clemenintegra.bom.service;

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
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FormulaProductoAprobacionIntegrationTest extends IntegrationTestMySqlContainer {

    @Autowired
    private FormulaProductoService formulaProductoService;

    @Autowired
    private FormulaProductoRepository formulaProductoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void aprobarFormulaActivaUnicaPorProducto() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("bom-aprobador")
                .clave("secret")
                .nombreCompleto("Usuario Aprobador")
                .correo("bom-aprobador@example.com")
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad BOM Aprobacion")
                .simbolo("UBA")
                .codigo("UBA")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria BOM Aprobacion")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-AP-1")
                .nombre("Producto Aprobacion")
                .descripcionProducto("Producto para aprobación")
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

        FormulaProducto formulaAnterior = formulaProductoRepository.save(FormulaProducto.builder()
                .producto(producto)
                .version("V1")
                .estado(EstadoFormula.APROBADA)
                .fechaCreacion(LocalDateTime.now().minusDays(2))
                .fechaActualizacion(LocalDateTime.now().minusDays(1))
                .activo(true)
                .creadoPor(usuario)
                .actualizadoPor(usuario)
                .build());

        FormulaProducto formulaNueva = formulaProductoRepository.save(FormulaProducto.builder()
                .producto(producto)
                .version("V2")
                .estado(EstadoFormula.EN_REVISION)
                .fechaCreacion(LocalDateTime.now().minusDays(1))
                .activo(false)
                .creadoPor(usuario)
                .build());

        formulaProductoService.cambiarEstado(formulaNueva.getId(), EstadoFormula.APROBADA, usuario.getId());

        FormulaProducto formulaAnteriorActualizada = formulaProductoRepository.findById(formulaAnterior.getId()).orElseThrow();
        FormulaProducto formulaNuevaActualizada = formulaProductoRepository.findById(formulaNueva.getId()).orElseThrow();

        assertThat(formulaNuevaActualizada.isActivo()).isTrue();
        assertThat(formulaNuevaActualizada.getEstado()).isEqualTo(EstadoFormula.APROBADA);
        assertThat(formulaNuevaActualizada.getFechaActualizacion()).isNotNull();
        assertThat(formulaNuevaActualizada.getActualizadoPor()).isNotNull();

        assertThat(formulaAnteriorActualizada.isActivo()).isFalse();
        assertThat(formulaAnteriorActualizada.getFechaActualizacion()).isNotNull();
        assertThat(formulaAnteriorActualizada.getActualizadoPor()).isNotNull();
    }
}
