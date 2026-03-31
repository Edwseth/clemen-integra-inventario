package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.valorizacion.AjusteValorizacionLoteRequestDTO;
import com.willyes.clemenintegra.inventario.dto.valorizacion.AjusteValorizacionLoteResponseDTO;
import com.willyes.clemenintegra.inventario.dto.valorizacion.ValorizacionElegibilidadResponseDTO;
import com.willyes.clemenintegra.inventario.model.AjusteValorizacionLote;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.AjusteValorizacionLoteRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AjusteValorizacionLoteServiceImplTest {

    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private AjusteValorizacionLoteRepository ajusteRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private BitacoraCambiosInventarioService bitacoraService;

    @InjectMocks
    private AjusteValorizacionLoteServiceImpl service;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        usuario = Usuario.builder().id(99L).nombreUsuario("contador").build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void precheckElegible() {
        LoteProducto lote = loteBase(new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(loteProductoRepository.findById(1L)).thenReturn(Optional.of(lote));

        ValorizacionElegibilidadResponseDTO response = service.evaluarElegibilidad(1L);

        assertTrue(response.eligible());
        assertEquals(new BigDecimal("10.000000"), response.snapshot().stockDisponible());
    }

    @Test
    void precheckNoElegiblePorStockCero() {
        LoteProducto lote = loteBase(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(loteProductoRepository.findById(1L)).thenReturn(Optional.of(lote));

        ValorizacionElegibilidadResponseDTO response = service.evaluarElegibilidad(1L);

        assertFalse(response.eligible());
        assertTrue(response.reglas().stream().anyMatch(r -> r.code().equals("STOCK_DISPONIBLE_MAYOR_A_CERO") && !r.passed()));
    }

    @Test
    void ajusteExitosoCostoCeroYStockPositivo() {
        mockAuth("INV_COSTEO_AJUSTE_WRITE");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        LoteProducto lote = loteBase(new BigDecimal("12"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(loteProductoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(lote));
        when(ajusteRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(ajusteRepository.save(any())).thenAnswer(inv -> {
            AjusteValorizacionLote a = inv.getArgument(0);
            a.setId(500L);
            a.setFechaAjuste(LocalDateTime.now());
            return a;
        });

        AjusteValorizacionLoteRequestDTO req = new AjusteValorizacionLoteRequestDTO(
                new BigDecimal("5.5"), "REGULARIZACION", "obs", "doc", false);

        AjusteValorizacionLoteResponseDTO response = service.ajustar(1L, req, "idem-1");

        assertEquals(500L, response.ajusteId());
        assertEquals(new BigDecimal("12.000000"), response.totalIngresadoNuevo());
        assertEquals(new BigDecimal("66.000000"), response.costoTotalNuevo());
        verify(bitacoraService).crear(any());
    }

    @Test
    void rechazoCostoPositivoSinOverride() {
        mockAuth("INV_COSTEO_AJUSTE_WRITE");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(ajusteRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());

        LoteProducto lote = loteBase(new BigDecimal("8"), BigDecimal.ZERO, new BigDecimal("1"), new BigDecimal("8"));
        when(loteProductoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(lote));

        AjusteValorizacionLoteRequestDTO req = new AjusteValorizacionLoteRequestDTO(
                new BigDecimal("7"), "REGULARIZACION", "obs", "doc", false);

        CustomBusinessException ex = assertThrows(CustomBusinessException.class,
                () -> service.ajustar(1L, req, "idem-2"));
        assertEquals(ApiErrorCode.LOTE_COSTO_YA_POSITIVO, ex.getCode());
    }

    @Test
    void overrideRequierePermisoSuperior() {
        mockAuth("INV_COSTEO_AJUSTE_WRITE");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(ajusteRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());

        LoteProducto lote = loteBase(new BigDecimal("8"), BigDecimal.ZERO, new BigDecimal("1"), new BigDecimal("8"));
        when(loteProductoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(lote));

        AjusteValorizacionLoteRequestDTO req = new AjusteValorizacionLoteRequestDTO(
                new BigDecimal("7"), "REGULARIZACION", "obs", "doc", true);

        CustomBusinessException ex = assertThrows(CustomBusinessException.class,
                () -> service.ajustar(1L, req, "idem-3"));
        assertEquals(ApiErrorCode.PERMISO_INSUFICIENTE, ex.getCode());
    }

    @Test
    void idempotenciaMismoPayloadRetornaMismoResultado() {
        AjusteValorizacionLote existente = AjusteValorizacionLote.builder()
                .id(77L)
                .lote(loteBase(new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .codigoLote("L-1")
                .producto(new Producto(10))
                .almacen(new Almacen(5))
                .costoUnitarioAnterior(BigDecimal.ZERO.setScale(6))
                .costoUnitarioNuevo(new BigDecimal("5.000000"))
                .costoTotalAnterior(BigDecimal.ZERO.setScale(6))
                .costoTotalNuevo(new BigDecimal("50.000000"))
                .totalIngresadoAnterior(BigDecimal.ZERO.setScale(6))
                .totalIngresadoNuevo(new BigDecimal("10.000000"))
                .idempotencyKey("idem-4")
                .payloadFingerprint("f")
                .usuario(usuario)
                .fechaAjuste(LocalDateTime.now())
                .build();

        AjusteValorizacionLoteRequestDTO req = new AjusteValorizacionLoteRequestDTO(
                new BigDecimal("5"), "m", "o", "d", false);

        when(ajusteRepository.findByIdempotencyKey("idem-4")).thenReturn(Optional.of(existente));
        AjusteValorizacionLoteServiceImpl spyService = spy(service);
        doReturn("f").when(spyService).calcularFingerprint(1L, req);

        AjusteValorizacionLoteResponseDTO response = spyService.ajustar(1L, req, "idem-4");
        assertEquals(77L, response.ajusteId());
        verify(loteProductoRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void idempotenciaMismaKeyPayloadDistintoGeneraConflicto() {
        AjusteValorizacionLote existente = AjusteValorizacionLote.builder()
                .payloadFingerprint("x")
                .idempotencyKey("idem-5")
                .build();
        when(ajusteRepository.findByIdempotencyKey("idem-5")).thenReturn(Optional.of(existente));

        AjusteValorizacionLoteRequestDTO req = new AjusteValorizacionLoteRequestDTO(
                new BigDecimal("5"), "m", "o", "d", false);

        AjusteValorizacionLoteServiceImpl spyService = spy(service);
        doReturn("y").when(spyService).calcularFingerprint(1L, req);

        CustomBusinessException ex = assertThrows(CustomBusinessException.class,
                () -> spyService.ajustar(1L, req, "idem-5"));
        assertEquals(ApiErrorCode.IDEMPOTENCY_KEY_REUTILIZADA_CON_PAYLOAD_DISTINTO, ex.getCode());
    }

    @Test
    void coherenciaCamposSiTotalIngresadoYaEsPositivo() {
        mockAuth("INV_COSTEO_AJUSTE_WRITE");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        LoteProducto lote = loteBase(new BigDecimal("15"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20"));
        when(loteProductoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(lote));
        when(ajusteRepository.findByIdempotencyKey("idem-6")).thenReturn(Optional.empty());
        when(ajusteRepository.save(any())).thenAnswer(inv -> {
            AjusteValorizacionLote a = inv.getArgument(0);
            a.setId(999L);
            a.setFechaAjuste(LocalDateTime.now());
            return a;
        });

        AjusteValorizacionLoteRequestDTO req = new AjusteValorizacionLoteRequestDTO(
                new BigDecimal("2"), "m", "o", "d", false);

        service.ajustar(1L, req, "idem-6");

        ArgumentCaptor<LoteProducto> loteCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteProductoRepository).save(loteCaptor.capture());
        LoteProducto actualizado = loteCaptor.getValue();
        assertEquals(new BigDecimal("20.000000"), actualizado.getTotalIngresadoMaterial());
        assertEquals(new BigDecimal("40.000000"), actualizado.getCostoTotalMaterialIngresado());
        assertEquals(new BigDecimal("2.000000"), actualizado.getCostoUnitarioMaterial());
    }

    @Test
    void auditoriaSePersiste() {
        mockAuth("INV_COSTEO_AJUSTE_WRITE");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        LoteProducto lote = loteBase(new BigDecimal("5"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(loteProductoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(lote));
        when(ajusteRepository.findByIdempotencyKey("idem-7")).thenReturn(Optional.empty());
        when(ajusteRepository.save(any())).thenAnswer(inv -> {
            AjusteValorizacionLote a = inv.getArgument(0);
            a.setId(12L);
            a.setFechaAjuste(LocalDateTime.now());
            return a;
        });

        AjusteValorizacionLoteRequestDTO req = new AjusteValorizacionLoteRequestDTO(
                new BigDecimal("1"), "m", "o", "d", false);

        service.ajustar(1L, req, "idem-7");

        verify(ajusteRepository).save(any(AjusteValorizacionLote.class));
    }

    private LoteProducto loteBase(BigDecimal stockLote,
                                  BigDecimal stockReservado,
                                  BigDecimal costoUnit,
                                  BigDecimal totalIngresado) {
        Producto producto = new Producto(10);
        producto.setNombre("Materia X");
        Almacen almacen = new Almacen(5);
        almacen.setNombre("Principal");

        return LoteProducto.builder()
                .id(1L)
                .codigoLote("L-1")
                .producto(producto)
                .almacen(almacen)
                .estado(EstadoLote.LIBERADO)
                .stockLote(stockLote)
                .stockReservado(stockReservado)
                .costoUnitarioMaterial(costoUnit)
                .costoTotalMaterialIngresado(BigDecimal.ZERO)
                .totalIngresadoMaterial(totalIngresado)
                .build();
    }

    private void mockAuth(String... authorities) {
        List<SimpleGrantedAuthority> auths = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", "n/a", auths)
        );
    }
}
