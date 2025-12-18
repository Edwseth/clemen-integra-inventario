package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.inventario.service.OrdenCompraService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.inventario.controller.OrdenCompraController;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraMapper;
import com.willyes.clemenintegra.inventario.mapper.RecepcionOCMapper;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import com.willyes.clemenintegra.inventario.repository.RecepcionOCRepository;
import com.willyes.clemenintegra.inventario.service.HistorialEstadoOrdenService;
import com.willyes.clemenintegra.inventario.service.OrdenCompraPdfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class OrdenCompraControllerTransicionesTest {

    @Mock
    private OrdenCompraRepository ordenCompraRepository;
    @Mock
    private OrdenCompraDetalleRepository detalleRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private OrdenCompraService ordenCompraService;
    @Mock
    private HistorialEstadoOrdenService historialEstadoOrdenService;
    @Mock
    private RecepcionOCRepository recepcionOCRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private MovimientoInventarioMapper movimientoInventarioMapper;
    @Mock
    private RecepcionOCMapper recepcionOCMapper;
    @Mock
    private OrdenCompraPdfService ordenCompraPdfService;
    @Mock
    private OrdenCompraMapper ordenCompraMapper;
    @InjectMocks
    private OrdenCompraController controller;

    @Test
    void transicionesEndpointRetornaLista() {
        OrdenCompra orden = OrdenCompra.builder()
                .id(10)
                .estado(EstadoOrdenCompra.CREADA)
                .build();
        when(ordenCompraService.buscarPorIdConDetalles(10L)).thenReturn(Optional.of(orden));
        when(ordenCompraService.transicionesPermitidas(any(), any()))
                .thenReturn(List.of(EstadoOrdenCompra.ENVIADA, EstadoOrdenCompra.CANCELADA));

        ResponseEntity<List<String>> response = controller.obtenerTransiciones(
                10L,
                usuario(RolUsuario.ROL_COMPRADOR));

        assertEquals(List.of("ENVIADA", "CANCELADA"), response.getBody());
        verify(ordenCompraService).buscarPorIdConDetalles(10L);
        verify(ordenCompraService).transicionesPermitidas(any(), any());
    }

    @Test
    void transicionesEndpointConRolNoPermitidoRetorna403() {
        OrdenCompra orden = OrdenCompra.builder()
                .id(11)
                .estado(EstadoOrdenCompra.CREADA)
                .build();
        when(ordenCompraService.buscarPorIdConDetalles(11L)).thenReturn(Optional.of(orden));
        when(ordenCompraService.transicionesPermitidas(any(), any()))
                .thenThrow(new CustomBusinessException(ApiErrorCode.ROL_INSUFICIENTE, "ROL_SIN_PERMISO_ESTADO"));

        CustomBusinessException ex = assertThrows(CustomBusinessException.class, () ->
                controller.obtenerTransiciones(11L, usuario(RolUsuario.ROL_ALMACENISTA)));

        assertEquals(ApiErrorCode.ROL_INSUFICIENTE, ex.getCode());
        verify(ordenCompraService).buscarPorIdConDetalles(11L);
        verify(ordenCompraService).transicionesPermitidas(any(), any());
    }

    private CustomUserDetails usuario(RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setRol(rol);
        usuario.setNombreUsuario("tester");
        usuario.setClave("pwd");
        usuario.setNombreCompleto("Tester");
        usuario.setActivo(true);
        usuario.setBloqueado(false);
        return new CustomUserDetails(usuario);
    }
}
