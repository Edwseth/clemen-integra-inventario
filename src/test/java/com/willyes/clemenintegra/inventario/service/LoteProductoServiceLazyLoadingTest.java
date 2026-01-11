package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UbicacionFisica;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.CategoriaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UbicacionFisicaRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LoteProductoServiceLazyLoadingTest {

    @Autowired
    private LoteProductoService loteProductoService;
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
    private UbicacionFisicaRepository ubicacionFisicaRepository;
    @Autowired
    private OrdenProduccionRepository ordenProduccionRepository;
    @Autowired
    private LoteProductoRepository loteProductoRepository;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private JavaMailSender javaMailSender;


    @Test
    void listarTodosPrecargaRelacionesParaMapper() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("liberador")
                .clave("secret")
                .nombreCompleto("Usuario Liberador")
                .correo("liberador@example.com")
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad")
                .simbolo("UND")
                .codigo("UND")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria Lote")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU-LOTE")
                .nombre("Producto Lote")
                .descripcionProducto("Producto para lote")
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

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Almacen Lote")
                .ubicacion("Zona Lote")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        UbicacionFisica ubicacion = ubicacionFisicaRepository.save(UbicacionFisica.builder()
                .almacen(almacen)
                .codigo("UB-001")
                .descripcion("Ubicacion principal")
                .activo(true)
                .build());

        OrdenProduccion ordenProduccion = ordenProduccionRepository.save(OrdenProduccion.builder()
                .codigoOrden("OP-001")
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(BigDecimal.ONE)
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .estado(EstadoProduccion.EN_PROCESO)
                .producto(producto)
                .unidadMedida(unidad)
                .responsable(usuario)
                .build());

        LoteProducto lotePsOrigen = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LP-ORIGEN")
                .stockLote(BigDecimal.TEN)
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacen)
                .build());

        loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("LP-001")
                .stockLote(BigDecimal.ONE)
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacen)
                .usuarioLiberador(usuario)
                .lotePsOrigen(lotePsOrigen)
                .ubicacionFisica(ubicacion)
                .ordenProduccion(ordenProduccion)
                .build());

        Page<LoteProductoResponseDTO> result = loteProductoService.listarTodos(
                null,
                null,
                null,
                false,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        LoteProductoResponseDTO dto = result.getContent().stream()
                .filter(item -> "LP-001".equals(item.getCodigoLote()))
                .findFirst()
                .orElseThrow();
        assertThat(dto.getNombreAlmacen()).isEqualTo("Almacen Lote");
        assertThat(dto.getNombreProducto()).isEqualTo("Producto Lote");
        assertThat(dto.getCodigoOrdenProduccion()).isEqualTo("OP-001");
    }
}
