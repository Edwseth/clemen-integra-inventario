package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.LoteUbicacionPicklistProjection;
import com.willyes.clemenintegra.inventario.dto.PicklistDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudMovimientoServicePicklistTest {

    @Mock
    private SolicitudMovimientoRepository repository;
    @Mock
    private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteProductoRepository loteRepository;
    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private ReservaLoteService reservaLoteService;
    @Mock
    private LoteCalidadValidator loteCalidadValidator;

    @InjectMocks
    private SolicitudMovimientoServiceImpl service;

    @Test
    @DisplayName("generarPicklist usa lote y origen del detalle cuando existen")
    void generarPicklist_detalleIncluyeLoteYOrigen() throws Exception {
        SolicitudMovimiento solicitud = crearSolicitudConDetalle(
                "EQUINACEA POLVO",
                "L-001",
                "Bodega MP",
                "Pasillo 1",
                "Pre-Bodega",
                "Zona F",
                BigDecimal.valueOf(5)
        );

        when(loteRepository.findUbicacionesFisicasPicklist(anySet(), anySet(), anySet()))
                .thenReturn(List.of(crearUbicacionProjection(
                        solicitud.getProducto().getId(),
                        solicitud.getDetalles().getFirst().getAlmacenOrigen().getId(),
                        "L-001",
                        "BOD-05-A01",
                        "PT.POSICION A01"
                )));
        when(repository.findWithDetalles(eq(1L), isNull(), isNull(), isNull(), eq(false), anyList()))
                .thenReturn(List.of(solicitud));

        List<SolicitudMovimientoServiceImpl.PicklistItem> items = service.buildPicklistItems(List.of(solicitud));

        assertThat(items).hasSize(1);
        SolicitudMovimientoServiceImpl.PicklistItem item = items.getFirst();
        assertThat(item.producto()).isEqualTo("EQUINACEA POLVO");
        assertThat(item.lote()).isEqualTo("L-001");
        assertThat(item.almacenOrigen()).isEqualTo("Bodega MP");
        assertThat(item.ubicacionOrigen()).isEqualTo("BOD-05-A01 - PT.POSICION A01");
        assertThat(item.almacenDestino()).isEqualTo("Pre-Bodega");
        assertThat(item.ubicacionDestino()).isEqualTo("-");

        PicklistDTO picklist = service.generarPicklist(1L, true);
        assertThat(picklist.getArchivo()).isNotEmpty();
    }

    @Test
    @DisplayName("generarPicklist crea una fila por cada detalle con su lote")
    void generarPicklist_multiDetalleGeneraMultiplesFilas() throws Exception {
        when(loteRepository.findUbicacionesFisicasPicklist(anySet(), anySet(), anySet()))
                .thenReturn(List.of());
        SolicitudMovimientoDetalle detalle1 = crearDetalle("L-001", "Bodega 1", "Rack A", BigDecimal.valueOf(2));
        SolicitudMovimientoDetalle detalle2 = crearDetalle("L-002", "Bodega 2", "Rack B", BigDecimal.valueOf(3));

        SolicitudMovimiento solicitud = crearSolicitudBase("Producto multi", "UNI");
        detalle1.setSolicitudMovimiento(solicitud);
        detalle2.setSolicitudMovimiento(solicitud);
        solicitud.setDetalles(List.of(detalle1, detalle2));

        List<SolicitudMovimientoServiceImpl.PicklistItem> items = service.buildPicklistItems(List.of(solicitud));

        assertThat(items)
                .hasSize(2)
                .extracting(SolicitudMovimientoServiceImpl.PicklistItem::lote)
                .containsExactlyInAnyOrder("L-001", "L-002");
        assertThat(items)
                .extracting(SolicitudMovimientoServiceImpl.PicklistItem::almacenOrigen)
                .contains("Bodega 1", "Bodega 2");
    }

    @Test
    @DisplayName("generarPicklist usa cabecera como respaldo cuando no hay detalles")
    void generarPicklist_sinDetallesUsaCabecera() throws Exception {
        when(loteRepository.findUbicacionesFisicasPicklist(anySet(), anySet(), anySet()))
                .thenReturn(List.of());
        Producto producto = new Producto();
        producto.setNombre("Producto sin detalle");
        UnidadMedida um = new UnidadMedida();
        um.setNombre("KG");
        producto.setUnidadMedida(um);
        producto.setId(99);

        Almacen origen = Almacen.builder().id(7).nombre("Almacen Cabecera").ubicacion("Zona C").build();
        LoteProducto loteCabecera = new LoteProducto();
        loteCabecera.setCodigoLote("CAB-001");

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(3L)
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .producto(producto)
                .lote(loteCabecera)
                .cantidad(BigDecimal.ONE)
                .almacenOrigen(origen)
                .estado(EstadoSolicitudMovimiento.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .detalles(List.of())
                .build();

        List<SolicitudMovimientoServiceImpl.PicklistItem> items = service.buildPicklistItems(List.of(solicitud));

        assertThat(items)
                .hasSize(1)
                .first()
                .satisfies(item -> {
                    assertThat(item.lote()).isEqualTo("CAB-001");
                    assertThat(item.almacenOrigen()).isEqualTo("Almacen Cabecera");
                    assertThat(item.ubicacionOrigen()).isEqualTo("-");
                    assertThat(item.producto()).isEqualTo("Producto sin detalle");
                });
    }

    @Test
    @DisplayName("generarPicklist usa ubicacion fisica cuando existe y '-' cuando no existe")
    void generarPicklist_ubicacionFisicaOptional() {
        SolicitudMovimiento solicitud = crearSolicitudConDetalle(
                "Producto ubicacion",
                "L-100",
                "Almacen A",
                "Pasillo Z",
                "Almacen B",
                "Zona X",
                BigDecimal.valueOf(2)
        );
        when(loteRepository.findUbicacionesFisicasPicklist(anySet(), anySet(), anySet()))
                .thenReturn(List.of(crearUbicacionProjection(
                        solicitud.getProducto().getId(),
                        solicitud.getDetalles().getFirst().getAlmacenOrigen().getId(),
                        "L-100",
                        "BOD-05-A01",
                        "PT.POSICION A01"
                )));

        List<SolicitudMovimientoServiceImpl.PicklistItem> items = service.buildPicklistItems(List.of(solicitud));

        assertThat(items)
                .hasSize(1)
                .first()
                .satisfies(item -> {
                    assertThat(item.ubicacionOrigen()).isEqualTo("BOD-05-A01 - PT.POSICION A01");
                    assertThat(item.ubicacionDestino()).isEqualTo("-");
                });

        when(loteRepository.findUbicacionesFisicasPicklist(anySet(), anySet(), anySet()))
                .thenReturn(List.of(crearUbicacionProjection(
                        solicitud.getProducto().getId(),
                        solicitud.getDetalles().getFirst().getAlmacenOrigen().getId(),
                        "L-100",
                        null,
                        null
                )));

        List<SolicitudMovimientoServiceImpl.PicklistItem> itemsSinUbicacion = service.buildPicklistItems(List.of(solicitud));

        assertThat(itemsSinUbicacion)
                .hasSize(1)
                .first()
                .satisfies(item -> assertThat(item.ubicacionOrigen()).isEqualTo("-"));
    }

    private SolicitudMovimiento crearSolicitudConDetalle(String nombreProducto,
                                                         String codigoLote,
                                                         String nombreAlmacenOrigen,
                                                         String ubicacionAlmacenOrigen,
                                                         String nombreAlmacenDestino,
                                                         String ubicacionDestino,
                                                         BigDecimal cantidad) {
        SolicitudMovimiento solicitud = crearSolicitudBase(nombreProducto, "GRM");

        Almacen origen = Almacen.builder().id(10).nombre(nombreAlmacenOrigen).ubicacion(ubicacionAlmacenOrigen).build();
        Almacen destino = Almacen.builder().id(11).nombre(nombreAlmacenDestino).ubicacion(ubicacionDestino).build();
        LoteProducto lote = new LoteProducto();
        lote.setCodigoLote(codigoLote);

        SolicitudMovimientoDetalle detalle = SolicitudMovimientoDetalle.builder()
                .lote(lote)
                .cantidad(cantidad)
                .almacenOrigen(origen)
                .almacenDestino(destino)
                .build();
        detalle.setSolicitudMovimiento(solicitud);
        solicitud.setDetalles(List.of(detalle));

        return solicitud;
    }

    private SolicitudMovimientoDetalle crearDetalle(String codigoLote, String nombreAlmacen, String ubicacion, BigDecimal cantidad) {
        LoteProducto lote = new LoteProducto();
        lote.setCodigoLote(codigoLote);
        Almacen origen = Almacen.builder().id(20).nombre(nombreAlmacen).ubicacion(ubicacion).build();

        return SolicitudMovimientoDetalle.builder()
                .lote(lote)
                .cantidad(cantidad)
                .almacenOrigen(origen)
                .build();
    }

    private SolicitudMovimiento crearSolicitudBase(String nombreProducto, String nombreUnidad) {
        UnidadMedida um = new UnidadMedida();
        um.setNombre(nombreUnidad);

        Producto producto = new Producto();
        producto.setNombre(nombreProducto);
        producto.setUnidadMedida(um);
        producto.setId(1);

        OrdenProduccion ordenProduccion = OrdenProduccion.builder()
                .id(1L)
                .codigoOrden("OP-TEST")
                .fechaInicio(LocalDateTime.now())
                .build();

        return SolicitudMovimiento.builder()
                .id(1L)
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .producto(producto)
                .ordenProduccion(ordenProduccion)
                .usuarioSolicitante(new Usuario())
                .estado(EstadoSolicitudMovimiento.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .observaciones("Observacion prueba")
                .build();
    }

    private LoteUbicacionPicklistProjection crearUbicacionProjection(Integer productoId,
                                                                     Integer almacenId,
                                                                     String codigoLote,
                                                                     String codigo,
                                                                     String descripcion) {
        return new LoteUbicacionPicklistProjection() {
            @Override
            public Integer getProductoId() {
                return productoId;
            }

            @Override
            public Integer getAlmacenId() {
                return almacenId;
            }

            @Override
            public String getCodigoLote() {
                return codigoLote;
            }

            @Override
            public String getUbicacionCodigo() {
                return codigo;
            }

            @Override
            public String getUbicacionDescripcion() {
                return descripcion;
            }
        };
    }

}
