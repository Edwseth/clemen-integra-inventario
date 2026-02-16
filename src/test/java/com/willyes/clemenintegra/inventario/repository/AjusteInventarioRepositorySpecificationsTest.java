package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.AjusteInventario;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.service.spec.AjusteInventarioSpecifications;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.flyway.enabled=false",
        "DB_SECURPASS=dummy",
        "DB_SECURNAME=dummy"
})
class AjusteInventarioRepositorySpecificationsTest {

    @Autowired
    private AjusteInventarioRepository ajusteInventarioRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Producto productoA;
    private Producto productoB;
    private Almacen almacenA;
    private Almacen almacenB;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = entityManager.persist(Usuario.builder()
                .nombreUsuario("tester-ajustes")
                .clave("clave")
                .nombreCompleto("Usuario Ajustes")
                .correo("ajustes@example.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = entityManager.persist(UnidadMedida.builder()
                .nombre("Unidad")
                .nombrePlural("Unidades")
                .simbolo("UND")
                .codigo("UND")
                .simboloImpresion("UND")
                .build());

        CategoriaProducto categoria = entityManager.persist(CategoriaProducto.builder()
                .nombre("Categoria Ajustes")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        productoA = entityManager.persist(crearProducto("SKU-AJ-1", "Producto A", unidad, categoria));
        productoB = entityManager.persist(crearProducto("SKU-AJ-2", "Producto B", unidad, categoria));

        almacenA = entityManager.persist(Almacen.builder()
                .nombre("Almacen A")
                .ubicacion("Zona A")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        almacenB = entityManager.persist(Almacen.builder()
                .nombre("Almacen B")
                .ubicacion("Zona B")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());
    }

    @Test
    @DisplayName("sin filtros devuelve todos los ajustes paginados")
    void sinFiltrosDevuelveTodos() {
        guardarAjuste(productoA, almacenA, LocalDateTime.of(2026, 2, 13, 10, 0), "A1");
        guardarAjuste(productoA, almacenB, LocalDateTime.of(2026, 2, 14, 10, 0), "A2");
        guardarAjuste(productoB, almacenA, LocalDateTime.of(2026, 2, 15, 10, 0), "A3");

        Specification<AjusteInventario> spec = Specification
                .where(AjusteInventarioSpecifications.hasProductoId(null))
                .and(AjusteInventarioSpecifications.hasAlmacenId(null))
                .and(AjusteInventarioSpecifications.fechaBetween(null, null));

        Page<AjusteInventario> page = ajusteInventarioRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("productoId filtra solo ajustes del producto solicitado")
    void filtraPorProductoId() {
        AjusteInventario esperado = guardarAjuste(productoA, almacenA, LocalDateTime.of(2026, 2, 14, 9, 0), "PA");
        guardarAjuste(productoB, almacenA, LocalDateTime.of(2026, 2, 14, 11, 0), "PB");

        Specification<AjusteInventario> spec = Specification
                .where(AjusteInventarioSpecifications.hasProductoId(productoA.getId().longValue()))
                .and(AjusteInventarioSpecifications.hasAlmacenId(null))
                .and(AjusteInventarioSpecifications.fechaBetween(null, null));

        Page<AjusteInventario> page = ajusteInventarioRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .hasSize(1)
                .extracting(AjusteInventario::getId)
                .containsExactly(esperado.getId());
    }

    @Test
    @DisplayName("almacenId filtra solo ajustes del almacén solicitado")
    void filtraPorAlmacenId() {
        AjusteInventario esperado = guardarAjuste(productoA, almacenB, LocalDateTime.of(2026, 2, 14, 9, 0), "ALM-B");
        guardarAjuste(productoA, almacenA, LocalDateTime.of(2026, 2, 14, 10, 0), "ALM-A");

        Specification<AjusteInventario> spec = Specification
                .where(AjusteInventarioSpecifications.hasProductoId(null))
                .and(AjusteInventarioSpecifications.hasAlmacenId(almacenB.getId().longValue()))
                .and(AjusteInventarioSpecifications.fechaBetween(null, null));

        Page<AjusteInventario> page = ajusteInventarioRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .hasSize(1)
                .extracting(AjusteInventario::getId)
                .containsExactly(esperado.getId());
    }

    @Test
    @DisplayName("fechaInicio y fechaFin filtran por rango inclusivo")
    void filtraPorRangoFechasInclusivo() {
        guardarAjuste(productoA, almacenA, LocalDateTime.of(2026, 2, 13, 23, 59, 59), "FUERA-INICIO");
        AjusteInventario inicioDia = guardarAjuste(productoA, almacenA, LocalDateTime.of(2026, 2, 14, 0, 0, 0), "INICIO");
        AjusteInventario finDia = guardarAjuste(productoA, almacenA, LocalDateTime.of(2026, 2, 15, 23, 59, 59), "FIN");
        guardarAjuste(productoA, almacenA, LocalDateTime.of(2026, 2, 16, 0, 0, 0), "FUERA-FIN");

        Specification<AjusteInventario> spec = Specification
                .where(AjusteInventarioSpecifications.hasProductoId(null))
                .and(AjusteInventarioSpecifications.hasAlmacenId(null))
                .and(AjusteInventarioSpecifications.fechaBetween(LocalDate.of(2026, 2, 14), LocalDate.of(2026, 2, 15)));

        Page<AjusteInventario> page = ajusteInventarioRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(AjusteInventario::getId)
                .containsExactlyInAnyOrder(inicioDia.getId(), finDia.getId());
    }

    private Producto crearProducto(String sku, String nombre, UnidadMedida unidad, CategoriaProducto categoria) {
        return Producto.builder()
                .codigoSku(sku)
                .nombre(nombre)
                .descripcionProducto("Producto de prueba")
                .stockMinimo(BigDecimal.ONE)
                .stockMinimoProveedor(BigDecimal.ZERO)
                .leadTimeCompraDias(1)
                .leadTimeProduccionDias(1)
                .stockSeguridad(BigDecimal.ZERO)
                .stockMaximoPlaneacion(BigDecimal.ZERO)
                .rendimientoUnidad(BigDecimal.ONE)
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
                .build();
    }

    private AjusteInventario guardarAjuste(Producto producto, Almacen almacen, LocalDateTime fecha, String motivo) {
        AjusteInventario ajuste = AjusteInventario.builder()
                .fecha(fecha)
                .cantidad(BigDecimal.ONE)
                .motivo(motivo)
                .observaciones("obs")
                .producto(producto)
                .almacen(almacen)
                .usuario(usuario)
                .build();

        ajusteInventarioRepository.save(ajuste);
        entityManager.flush();
        return ajuste;
    }
}
