package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.dto.InsumoAutocompleteDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.flyway.enabled=false",
        "DB_SECURPASS=dummy",
        "DB_SECURNAME=dummy"
})
class ProductoRepositoryAutocompleteTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Producto producto;
    private UnidadMedida unidad;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("tester")
                .clave("secret")
                .nombreCompleto("Test User")
                .correo("test@example.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .build());

        unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .simbolo("U")
                .codigo("U1")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("MP")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        producto = productoRepository.save(Producto.builder()
                .codigoSku("MP-001")
                .nombre("Insumo Prueba")
                .stockMinimo(BigDecimal.ONE)
                .activo(true)
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
    @DisplayName("buscarInsumosAutocomplete retorna campos mínimos para el autocomplete")
    void buscarInsumosAutocomplete_cargaUnidadMedida() {
        Page<InsumoAutocompleteDTO> page = productoRepository.buscarInsumosAutocomplete(
                List.of(TipoCategoria.MATERIA_PRIMA),
                "MP",
                PageRequest.of(0, 5)
        );

        assertThat(page.getContent()).hasSize(1);
        InsumoAutocompleteDTO resultado = page.getContent().get(0);
        assertThat(resultado.getId()).isEqualTo(producto.getId());
        assertThat(resultado.getSku()).isEqualTo("MP-001");
        assertThat(resultado.getUnidad()).isEqualTo("Unidad");
        assertThat(resultado.getUnidadMedidaId()).isEqualTo(unidad.getId());
        assertThat(resultado.getUnidadMedidaNombre()).isEqualTo("Unidad");
    }
}
