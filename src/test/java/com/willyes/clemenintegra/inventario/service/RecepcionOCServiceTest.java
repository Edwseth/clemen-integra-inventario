package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({CodigoRecepcionServiceImpl.class, RecepcionOCServiceImpl.class})
@TestPropertySource(properties = "spring.jpa.defer-datasource-initialization=true")
class RecepcionOCServiceTest {

    @Autowired
    private RecepcionOCService recepcionOCService;

    @Autowired
    private EntityManager entityManager;

    private OrdenCompra ordenCompra;
    private Almacen almacenDestino;
    private Proveedor proveedor;
    private Usuario usuario;
    private Producto producto;
    private UnidadMedida unidadMedida;
    private CategoriaProducto categoriaProducto;

    @BeforeEach
    void setUp() {
        proveedor = Proveedor.builder()
                .nombre("Proveedor Test")
                .identificacion("123")
                .telefono("123")
                .email("prov@test.com")
                .direccion("Dir")
                .paginaWeb("web")
                .nombreContacto("Contacto")
                .activo(true)
                .build();
        entityManager.persist(proveedor);

        usuario = Usuario.builder()
                .nombreUsuario("usuario")
                .clave("clave")
                .nombreCompleto("Usuario Test")
                .correo("user@test.com")
                .rol(RolUsuario.ROL_ALMACENISTA)
                .activo(true)
                .bloqueado(false)
                .build();
        entityManager.persist(usuario);

        almacenDestino = Almacen.builder()
                .nombre("Almacen Central")
                .ubicacion("Ubicacion")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build();
        entityManager.persist(almacenDestino);

        unidadMedida = UnidadMedida.builder()
                .nombre("Unidad")
                .simbolo("u")
                .build();
        entityManager.persist(unidadMedida);

        categoriaProducto = CategoriaProducto.builder()
                .nombre("Materia Prima")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build();
        entityManager.persist(categoriaProducto);

        producto = Producto.builder()
                .codigoSku("SKU-1")
                .nombre("Producto 1")
                .descripcionProducto("Desc")
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .activo(true)
                .stockMinimo(BigDecimal.ZERO)
                .stockMinimoProveedor(BigDecimal.ZERO)
                .rendimientoUnidad(BigDecimal.ONE)
                .unidadMedida(unidadMedida)
                .categoriaProducto(categoriaProducto)
                .creadoPor(usuario)
                .build();
        entityManager.persist(producto);

        ordenCompra = OrdenCompra.builder()
                .codigoOrden("OC-1")
                .fechaOrden(LocalDateTime.now())
                .proveedor(proveedor)
                .estado(EstadoOrdenCompra.CREADA)
                .observaciones("Obs")
                .build();
        entityManager.persist(ordenCompra);

        OrdenCompraDetalle detalle = OrdenCompraDetalle.builder()
                .ordenCompra(ordenCompra)
                .producto(producto)
                .cantidad(new BigDecimal("1"))
                .valorUnitario(new BigDecimal("1"))
                .valorTotal(new BigDecimal("1"))
                .iva(new BigDecimal("0.00"))
                .cantidadRecibida(BigDecimal.ZERO)
                .build();
        entityManager.persist(detalle);
        ordenCompra.setDetalles(new ArrayList<>(List.of(detalle)));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void reutilizaCabeceraPorDiaYOrdenCompra() {
        LocalDate fecha = LocalDate.of(2025, 5, 21);
        RecepcionOC primera = recepcionOCService.findOrCreateCabecera(
                ordenCompra.getId(),
                almacenDestino.getId(),
                proveedor.getId(),
                usuario.getId(),
                fecha,
                "Obs inicial"
        );
        RecepcionOC segunda = recepcionOCService.findOrCreateCabecera(
                ordenCompra.getId(),
                almacenDestino.getId(),
                proveedor.getId(),
                usuario.getId(),
                fecha,
                "Obs distinta"
        );
        RecepcionOC otroDia = recepcionOCService.findOrCreateCabecera(
                ordenCompra.getId(),
                almacenDestino.getId(),
                proveedor.getId(),
                usuario.getId(),
                fecha.plusDays(1),
                null
        );

        assertThat(primera.getId()).isEqualTo(segunda.getId());
        assertThat(primera.getCodigo()).isEqualTo(segunda.getCodigo());
        assertThat(otroDia.getId()).isNotEqualTo(primera.getId());
        assertThat(otroDia.getCodigo()).isNotEqualTo(primera.getCodigo());
        assertThat(primera.getProveedor().getId()).isEqualTo(proveedor.getId());
        assertThat(primera.getOrdenCompra().getId()).isEqualTo(ordenCompra.getId());
    }
}
