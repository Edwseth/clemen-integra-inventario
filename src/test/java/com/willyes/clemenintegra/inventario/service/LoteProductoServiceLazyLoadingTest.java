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
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LoteProductoServiceLazyLoadingTest extends IntegrationTestMySqlContainer {

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

    @Test
    void listarTodosVencidosFiltraPorProductoIdYAlmacenId() {
        long sufijo = System.nanoTime();
        Producto productoA = crearProductoBasico("PROD-A-" + sufijo, "Producto A " + sufijo);
        Producto productoB = crearProductoBasico("PROD-B-" + sufijo, "Producto B " + sufijo);
        Almacen almacenA = crearAlmacenBasico("Almacen A " + sufijo);
        Almacen almacenB = crearAlmacenBasico("Almacen B " + sufijo);

        crearLoteVencido("LOT-A1-" + sufijo, productoA, almacenA);
        crearLoteVencido("LOT-A2-" + sufijo, productoA, almacenB);
        crearLoteVencido("LOT-B1-" + sufijo, productoB, almacenA);

        Page<LoteProductoResponseDTO> result = loteProductoService.listarTodos(
                null,
                productoA.getId(),
                null,
                null,
                almacenA.getId(),
                true,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(LoteProductoResponseDTO::getCodigoLote)
                .containsExactly("LOT-A1-" + sufijo);
    }

    @Test
    void listarTodosVencidosFiltraPorTextoProductoComoAntes() {
        long sufijo = System.nanoTime();
        Producto productoA = crearProductoBasico("HARINA-" + sufijo, "Harina especial " + sufijo);
        Producto productoB = crearProductoBasico("AZUCAR-" + sufijo, "Azucar blanca " + sufijo);
        Almacen almacen = crearAlmacenBasico("Almacen Texto " + sufijo);

        crearLoteVencido("LOT-T1-" + sufijo, productoA, almacen);
        crearLoteVencido("LOT-T2-" + sufijo, productoB, almacen);

        Page<LoteProductoResponseDTO> result = loteProductoService.listarTodos(
                "harina",
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(LoteProductoResponseDTO::getCodigoLote)
                .containsExactly("LOT-T1-" + sufijo);
    }

    @Test
    void listarTodosPriorizaProductoIdSobreTexto() {
        long sufijo = System.nanoTime();
        Producto productoId = crearProductoBasico("ID-" + sufijo, "Producto ID " + sufijo);
        Producto productoTexto = crearProductoBasico("TXT-" + sufijo, "Texto objetivo " + sufijo);
        Almacen almacen = crearAlmacenBasico("Almacen Prioridad " + sufijo);

        crearLoteVencido("LOT-P1-" + sufijo, productoId, almacen);
        crearLoteVencido("LOT-P2-" + sufijo, productoTexto, almacen);

        Page<LoteProductoResponseDTO> result = loteProductoService.listarTodos(
                "texto",
                productoId.getId(),
                null,
                null,
                null,
                true,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(LoteProductoResponseDTO::getCodigoLote)
                .containsExactly("LOT-P1-" + sufijo);
    }

    private Producto crearProductoBasico(String sku, String nombre) {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("user-" + sku)
                .clave("secret")
                .nombreCompleto("Usuario " + sku)
                .correo(sku + "@example.com")
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Unidad " + sku)
                .simbolo("UND")
                .codigo("UND-" + sku)
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Categoria " + sku)
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        return productoRepository.save(Producto.builder()
                .codigoSku(sku)
                .nombre(nombre)
                .descripcionProducto("Producto " + nombre)
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
    }

    private Almacen crearAlmacenBasico(String nombre) {
        return almacenRepository.save(Almacen.builder()
                .nombre(nombre)
                .ubicacion("Zona " + nombre)
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());
    }

    private LoteProducto crearLoteVencido(String codigo, Producto producto, Almacen almacen) {
        return loteProductoRepository.save(LoteProducto.builder()
                .codigoLote(codigo)
                .stockLote(BigDecimal.ONE)
                .estado(EstadoLote.DISPONIBLE)
                .fechaVencimiento(LocalDateTime.now().minusDays(1))
                .producto(producto)
                .almacen(almacen)
                .build());
    }

}
