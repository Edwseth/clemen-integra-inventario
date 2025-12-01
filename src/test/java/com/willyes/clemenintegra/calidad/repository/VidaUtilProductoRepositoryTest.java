package com.willyes.clemenintegra.calidad.repository;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.VidaUtilProductoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=none",
        "spring.jpa.defer-datasource-initialization=true",
        "DB_SECURPASS=dummy",
        "DB_SECURNAME=dummy"
})
@Sql(scripts = "classpath:sql/vida-util-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class VidaUtilProductoRepositoryTest {

    @Autowired
    private VidaUtilProductoRepository vidaUtilProductoRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Guardar vida útil con PK explícita no provoca AssertionFailure")
    void guardarDebePersistirConProductoIdAsignado() {
        Usuario usuario = Usuario.builder()
                .nombreUsuario("tester")
                .clave("clave")
                .nombreCompleto("Usuario Test")
                .correo("tester@example.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .build();
        entityManager.persist(usuario);

        UnidadMedida unidad = UnidadMedida.builder()
                .nombre("Unidad")
                .nombrePlural("Unidades")
                .simbolo("U")
                .codigo("U01")
                .build();
        entityManager.persist(unidad);

        CategoriaProducto categoria = CategoriaProducto.builder()
                .nombre("PT")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build();
        entityManager.persist(categoria);

        Producto producto = Producto.builder()
                .codigoSku("SKU-1")
                .nombre("Producto test")
                .stockMinimo(BigDecimal.ONE)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .modoControlInventario(ModoControlInventario.CONTROL_STOCK)
                .build();
        entityManager.persistAndFlush(producto);

        VidaUtilProducto vidaUtilProducto = new VidaUtilProducto();
        vidaUtilProducto.setProductoId(producto.getId());
        vidaUtilProducto.setProducto(producto);
        vidaUtilProducto.setSemanasVigencia(12);
        vidaUtilProducto.setActualizadoPor(usuario);
        vidaUtilProducto.setFechaActualizacion(LocalDateTime.now());

        assertThat(producto.getId()).isNotNull();
        assertThat(vidaUtilProducto.getProductoId()).isNotNull();

        VidaUtilProducto guardado = vidaUtilProductoRepository.saveAndFlush(vidaUtilProducto);

        Optional<VidaUtilProducto> encontrado = vidaUtilProductoRepository.findById(producto.getId());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getSemanasVigencia()).isEqualTo(12);
        assertThat(guardado.getProductoId()).isEqualTo(producto.getId());
    }
}
