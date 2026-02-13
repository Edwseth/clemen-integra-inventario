package com.willyes.clemenintegra.inventario.regularizacion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.regularizacion.dto.AjusteLoteDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadRequestDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadResponseDTO;
import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidadOperacion;
import com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadOperacionRepository;
import com.willyes.clemenintegra.inventario.regularizacion.service.impl.RegularizacionTrazabilidadServiceImpl;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegularizacionTrazabilidadServiceImplTest {

    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private MovimientoInventarioService movimientoInventarioService;
    @Mock
    private RegularizacionTrazabilidadOperacionRepository operacionRepository;

    @InjectMocks
    private RegularizacionTrazabilidadServiceImpl service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "allowNegativeStock", false);
        ReflectionTestUtils.setField(service, "adjuntoObligatorio", false);
        ReflectionTestUtils.setField(service, "umbralAdjunto", new BigDecimal("1000"));
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper().findAndRegisterModules());

        usuario = Usuario.builder()
                .id(99L)
                .rol(RolUsuario.ROL_CONTADOR)
                .nombreUsuario("contador")
                .clave("x")
                .nombreCompleto("Contador")
                .correo("c@erp.test")
                .activo(true)
                .bloqueado(false)
                .build();

        when(ordenProduccionRepository.existsById(1L)).thenReturn(true);
        when(productoRepository.existsById(10L)).thenReturn(true);

        LoteProducto l1 = new LoteProducto();
        Producto producto = new Producto();
        producto.setId(10);
        l1.setId(100L);
        l1.setProducto(producto);

        LoteProducto l2 = new LoteProducto();
        l2.setId(101L);
        l2.setProducto(producto);

        when(loteProductoRepository.findById(100L)).thenReturn(Optional.of(l1));
        lenient().when(loteProductoRepository.findById(101L)).thenReturn(Optional.of(l2));
    }

    @Test
    void creaMovimientosAjusteYAsociaOrdenProduccion() {
        RegularizacionTrazabilidadRequestDTO request = buildRequest();
        when(operacionRepository.findByIdempotencyKey("k-op")).thenReturn(Optional.empty());
        when(operacionRepository.save(any())).thenAnswer(inv -> {
            RegularizacionTrazabilidadOperacion op = inv.getArgument(0);
            if (op.getId() == null) {
                op.setId(77L);
            }
            return op;
        });
        when(movimientoInventarioService.registrarMovimiento(any(), eq("k-op:1")))
                .thenReturn(MovimientoInventarioResponseDTO.builder().id(501L).build());
        when(movimientoInventarioService.registrarMovimiento(any(), eq("k-op:2")))
                .thenReturn(MovimientoInventarioResponseDTO.builder().id(502L).build());

        RegularizacionTrazabilidadResponseDTO response = service.regularizarPorOP(request, "k-op", usuario);

        assertThat(response.movimientos()).hasSize(2);
        assertThat(response.movimientos().get(0).tipoMovimiento()).isEqualTo("AJUSTE");
        assertThat(response.movimientos().get(0).clasificacion()).isEqualTo("AJUSTE_POSITIVO");
        assertThat(response.movimientos().get(1).clasificacion()).isEqualTo("AJUSTE_NEGATIVO");
        assertThat(response.ordenProduccionId()).isEqualTo(1L);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(movimientoInventarioService, times(2)).registrarMovimiento(any(), keyCaptor.capture());
        assertThat(keyCaptor.getAllValues()).containsExactly("k-op:1", "k-op:2");
    }

    @Test
    void noPermiteStockNegativoPorDefecto() {
        RegularizacionTrazabilidadRequestDTO request = new RegularizacionTrazabilidadRequestDTO(
                1L, 10L, 2L, 3L, "DOC", "Observaciones válidas de prueba",
                null, false,
                List.of(new AjusteLoteDTO(100L, "NEGATIVO", new BigDecimal("999")))
        );
        when(operacionRepository.findByIdempotencyKey("k-neg")).thenReturn(Optional.empty());
        when(operacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movimientoInventarioService.registrarMovimiento(any(), anyString()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                        "STOCK_LOTE_INSUFICIENTE"
                ));

        assertThatThrownBy(() -> service.regularizarPorOP(request, "k-neg", usuario))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("STOCK_LOTE_INSUFICIENTE");
    }

    @Test
    void idempotenciaMismaKeyMismoPayloadDevuelveMismoResultado() throws Exception {
        RegularizacionTrazabilidadResponseDTO resultado = RegularizacionTrazabilidadResponseDTO.builder()
                .operacionId(44L)
                .idempotencyKey("k-idem")
                .ordenProduccionId(1L)
                .productoId(10L)
                .tipoOperacion("REGULARIZACION_TRAZABILIDAD_OP")
                .movimientos(List.of())
                .registradoPorId(99L)
                .fecha(LocalDateTime.now())
                .build();
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(resultado);

        RegularizacionTrazabilidadOperacion op = RegularizacionTrazabilidadOperacion.builder()
                .id(44L)
                .idempotencyKey("k-idem")
                .requestHash((String) ReflectionTestUtils.invokeMethod(service, "calcularHashRequest", buildRequest()))
                .estado("APLICADA")
                .ordenProduccionId(1L)
                .productoId(10L)
                .creadoPor(usuario)
                .createdAt(LocalDateTime.now())
                .resultadoJson(json)
                .build();

        when(operacionRepository.findByIdempotencyKey("k-idem")).thenReturn(Optional.of(op));

        RegularizacionTrazabilidadResponseDTO response = service.regularizarPorOP(buildRequest(), "k-idem", usuario);

        assertThat(response.operacionId()).isEqualTo(44L);
        verifyNoInteractions(movimientoInventarioService);
    }

    @Test
    void idempotenciaMismaKeyPayloadDistintoDevuelve409() {
        RegularizacionTrazabilidadOperacion op = RegularizacionTrazabilidadOperacion.builder()
                .id(44L)
                .idempotencyKey("k-idem")
                .requestHash("hash-base")
                .estado("APLICADA")
                .ordenProduccionId(1L)
                .productoId(10L)
                .creadoPor(usuario)
                .createdAt(LocalDateTime.now())
                .resultadoJson("{}")
                .build();

        when(operacionRepository.findByIdempotencyKey("k-idem")).thenReturn(Optional.of(op));

        assertThatThrownBy(() -> service.regularizarPorOP(buildRequest(), "k-idem", usuario))
                .isInstanceOf(CustomBusinessException.class)
                .extracting(ex -> ((CustomBusinessException) ex).getCode())
                .isEqualTo(ApiErrorCode.IDEMPOTENCY_KEY_REUSADA_CON_OTRO_PAYLOAD);
    }

    private RegularizacionTrazabilidadRequestDTO buildRequest() {
        return new RegularizacionTrazabilidadRequestDTO(
                1L,
                10L,
                2L,
                3L,
                "DOC-1",
                "Observaciones válidas de prueba",
                null,
                false,
                List.of(
                        new AjusteLoteDTO(100L, "POSITIVO", new BigDecimal("2.5")),
                        new AjusteLoteDTO(101L, "NEGATIVO", new BigDecimal("1.5"))
                )
        );
    }
}
