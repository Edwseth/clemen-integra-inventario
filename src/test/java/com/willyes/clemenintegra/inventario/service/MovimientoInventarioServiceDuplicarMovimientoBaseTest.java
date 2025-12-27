package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceDuplicarMovimientoBaseTest {

    @Mock private AlmacenRepository almacenRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private OrdenCompraRepository ordenCompraRepository;
    @Mock private OrdenCompraService ordenCompraService;
    @Mock private LoteProductoRepository loteProductoRepository;
    @Mock private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock private MovimientoInventarioRepository repository;
    @Mock private MovimientoInventarioMapper mapper;
    @Mock private UsuarioService usuarioService;
    @Mock private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock private InventoryCatalogResolver catalogResolver;
    @Mock private ReservaLoteService reservaLoteService;
    @Mock private ReservaLoteRepository reservaLoteRepository;
    @Mock private RecepcionOCService recepcionOCService;
    @Mock private LoteCalidadValidator loteCalidadValidator;
    @Mock private UbicacionFisicaRepository ubicacionFisicaRepository;
    @Mock private EtapaProduccionRepository etapaProduccionRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @Test
    void duplicarMovimientoBase_preservaEtapaProduccion() {
        EtapaProduccion etapa = new EtapaProduccion();
        etapa.setId(99L);

        MovimientoInventario base = new MovimientoInventario();
        base.setOrdenProduccionEtapa(etapa);

        MovimientoInventario copia = ReflectionTestUtils.invokeMethod(
                service,
                "duplicarMovimientoBase",
                base,
                new LoteProducto(),
                new BigDecimal("1.0")
        );

        assertThat(copia).isNotNull();
        assertThat(copia.getOrdenProduccionEtapa()).isEqualTo(etapa);
    }
}
