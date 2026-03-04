package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDetalleRequestDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO;
import com.willyes.clemenintegra.inventario.model.HistorialEstadoOrden;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import com.willyes.clemenintegra.inventario.repository.HistorialEstadoOrdenRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final HistorialEstadoOrdenRepository historialEstadoOrdenRepository;

    /**
     * Busca una orden de compra por su ID incluyendo proveedor y detalles.
     * @param id ID de la orden
     * @return Optional con la orden completa, o vacío si no existe
     */
    public Optional<OrdenCompra> buscarPorIdConDetalles(Long id) {
        return ordenCompraRepository.findByIdWithDetalles(id);
    }

    public Page<OrdenCompraResponseDTO> listar(Pageable pageable, boolean atrasadas) {
        return listarFiltrado(pageable, atrasadas, null, null, null);
    }

    public Page<OrdenCompraResponseDTO> listarFiltrado(Pageable pageable,
                                                       boolean atrasadas,
                                                       EstadoOrdenCompra estado,
                                                       TipoOrdenCompra tipo,
                                                       String proveedor) {
        String proveedorNormalizado = (proveedor == null || proveedor.isBlank()) ? null : proveedor.trim();
        return ordenCompraRepository.findListadoFiltrado(
                pageable,
                atrasadas,
                EnumSet.of(EstadoOrdenCompra.ENVIADA, EstadoOrdenCompra.PARCIALMENTE_RECIBIDA),
                estado,
                tipo,
                proveedorNormalizado);
    }

    public Page<OrdenCompraResponseDTO> listarPorEstado(EstadoOrdenCompra estado, TipoOrdenCompra tipo, Pageable pageable) {
        return listarFiltrado(pageable, false, estado, tipo, null);
    }

    public String generarCodigoOrdenCompra() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefijo = "OC-CLEMEN-" + fecha;
        Long contador = ordenCompraRepository.countByCodigoOrdenStartingWith(prefijo);
        return prefijo + "-" + String.format("%02d", contador + 1);
    }

    public LocalDate calcularFechaCompromisoEntrega(List<OrdenCompraDetalleRequestDTO> detalles) {
        if (detalles == null) {
            return null;
        }
        return detalles.stream()
                .map(OrdenCompraDetalleRequestDTO::getFechaNecesidad)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Evalúa el estado de una orden según la cantidad recibida en sus detalles
     * y actualiza la entidad si corresponde.
     * @param orden Orden de compra a evaluar
     */
    public void evaluarYActualizarEstado(OrdenCompra orden) {
        boolean allReceived = orden.getDetalles().stream()
                .allMatch(d -> d.getCantidadRecibida().compareTo(d.getCantidad()) >= 0);
        boolean anyReceived = orden.getDetalles().stream()
                .anyMatch(d -> d.getCantidadRecibida().compareTo(java.math.BigDecimal.ZERO) > 0);

        EstadoOrdenCompra nuevoEstado = orden.getEstado();
        if (allReceived) {
            nuevoEstado = EstadoOrdenCompra.RECIBIDA_COMPLETAMENTE;
        } else if (anyReceived) {
            nuevoEstado = EstadoOrdenCompra.PARCIALMENTE_RECIBIDA;
        }

        if (nuevoEstado != orden.getEstado()) {
            orden.setEstado(nuevoEstado);
            ordenCompraRepository.save(orden);
        }
    }

    public HistorialEstadoOrden cambiarEstado(Long ordenId,
                                              EstadoOrdenCompra nuevoEstado,
                                              CustomUserDetails principal,
                                              String observaciones) {
        if (principal == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "USUARIO_NO_AUTENTICADO");
        }

        OrdenCompra orden = ordenCompraRepository.findByIdWithDetalles(ordenId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "ORDEN_NO_ENCONTRADA"));

        validarTransicion(orden, nuevoEstado);
        validarRol(principal, orden.getEstado(), nuevoEstado);

        orden.setEstado(nuevoEstado);
        ordenCompraRepository.save(orden);

        Usuario usuario = principal.getUsuario() != null ? principal.getUsuario() : new Usuario();
        if (usuario.getId() == null) {
            usuario.setId(principal.getId());
        }

        HistorialEstadoOrden historial = HistorialEstadoOrden.builder()
                .ordenCompra(orden)
                .estado(nuevoEstado)
                .fechaCambio(LocalDateTime.now())
                .cambiadoPor(usuario)
                .observaciones(observaciones)
                .build();

        return historialEstadoOrdenRepository.save(historial);
    }

    public List<EstadoOrdenCompra> transicionesPermitidas(OrdenCompra orden,
                                                          Collection<String> authorities) {
        if (orden == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "ORDEN_NO_ENCONTRADA");
        }

        RolUsuario rol = extraerRol(authorities);
        validarRolModulo(rol);

        List<EstadoOrdenCompra> permitidas = new ArrayList<>();
        for (EstadoOrdenCompra candidato : EstadoOrdenCompra.values()) {
            try {
                validarTransicion(orden, candidato);
                validarRol(orden.getEstado(), candidato, rol);
                permitidas.add(candidato);
            } catch (CustomBusinessException ignored) {
            }
        }
        return permitidas;
    }

    // Métodos adicionales futuros: crear, editar, anular, etc.

    private void validarTransicion(OrdenCompra orden, EstadoOrdenCompra nuevoEstado) {
        EstadoOrdenCompra estadoActual = orden.getEstado();
        if (estadoActual == null || nuevoEstado == null || estadoActual == nuevoEstado) {
            throw new CustomBusinessException(ApiErrorCode.OC_TRANSICION_INVALIDA, "TRANSICION_NO_PERMITIDA");
        }

        BigDecimal totalRecibido = orden.getDetalles() != null
                ? orden.getDetalles().stream()
                .map(d -> d.getCantidadRecibida() != null ? d.getCantidadRecibida() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        switch (estadoActual) {
            case CREADA -> {
                if (!(nuevoEstado == EstadoOrdenCompra.ENVIADA || nuevoEstado == EstadoOrdenCompra.CANCELADA)) {
                    throw new CustomBusinessException(ApiErrorCode.OC_TRANSICION_INVALIDA, "TRANSICION_NO_PERMITIDA");
                }
            }
            case ENVIADA -> {
                if (!(nuevoEstado == EstadoOrdenCompra.CANCELADA || nuevoEstado == EstadoOrdenCompra.RECHAZADA)) {
                    throw new CustomBusinessException(ApiErrorCode.OC_TRANSICION_INVALIDA, "TRANSICION_NO_PERMITIDA");
                }
            }
            case PARCIALMENTE_RECIBIDA -> {
                boolean tieneRecibido = totalRecibido.compareTo(BigDecimal.ZERO) > 0;
                if (nuevoEstado == EstadoOrdenCompra.RECHAZADA && tieneRecibido) {
                    throw new CustomBusinessException(ApiErrorCode.OC_TRANSICION_INVALIDA, "OC_CON_RECEPCION_NO_PUEDE_RECHAZARSE");
                }
                if (nuevoEstado == EstadoOrdenCompra.CANCELADA && tieneRecibido) {
                    throw new CustomBusinessException(ApiErrorCode.OC_TRANSICION_INVALIDA, "OC_CON_RECEPCION_NO_PUEDE_CANCELARSE");
                }
                if (!(nuevoEstado == EstadoOrdenCompra.CANCELADA || nuevoEstado == EstadoOrdenCompra.RECHAZADA)) {
                    throw new CustomBusinessException(ApiErrorCode.OC_TRANSICION_INVALIDA, "TRANSICION_NO_PERMITIDA");
                }
            }
            case RECIBIDA_COMPLETAMENTE -> {
                if (nuevoEstado != EstadoOrdenCompra.CERRADA) {
                    throw new CustomBusinessException(ApiErrorCode.OC_TRANSICION_INVALIDA, "TRANSICION_NO_PERMITIDA");
                }
            }
            default -> throw new CustomBusinessException(ApiErrorCode.OC_TRANSICION_INVALIDA, "ESTADO_FINAL_NO_EDITABLE");
        }
    }

    private void validarRol(CustomUserDetails principal, EstadoOrdenCompra estadoActual, EstadoOrdenCompra nuevoEstado) {
        RolUsuario rol = principal.getUsuario() != null ? principal.getUsuario().getRol() : null;
        validarRol(estadoActual, nuevoEstado, rol);
    }

    private void validarRol(EstadoOrdenCompra estadoActual, EstadoOrdenCompra nuevoEstado, RolUsuario rol) {
        if (rol == null) {
            throw new CustomBusinessException(ApiErrorCode.ROL_INSUFICIENTE, "ROL_NO_DEFINIDO");
        }
        boolean esSuper = rol == RolUsuario.ROL_SUPER_ADMIN;
        boolean esComprador = rol == RolUsuario.ROL_COMPRADOR;
        boolean esJefeAlmacenes = rol == RolUsuario.ROL_JEFE_ALMACENES;

        if (estadoActual == EstadoOrdenCompra.CREADA) {
            if ((nuevoEstado == EstadoOrdenCompra.ENVIADA || nuevoEstado == EstadoOrdenCompra.CANCELADA)
                    && (esComprador || esSuper)) {
                return;
            }
        }

        if (estadoActual == EstadoOrdenCompra.ENVIADA || estadoActual == EstadoOrdenCompra.PARCIALMENTE_RECIBIDA) {
            if (nuevoEstado == EstadoOrdenCompra.CANCELADA && (esComprador || esSuper)) {
                return;
            }
            if (nuevoEstado == EstadoOrdenCompra.RECHAZADA && esSuper) {
                return;
            }
        }

        if (estadoActual == EstadoOrdenCompra.RECIBIDA_COMPLETAMENTE
                && nuevoEstado == EstadoOrdenCompra.CERRADA
                && (esJefeAlmacenes || esSuper)) {
            return;
        }

        throw new CustomBusinessException(ApiErrorCode.ROL_INSUFICIENTE, "ROL_SIN_PERMISO_ESTADO");
    }

    private RolUsuario extraerRol(Collection<String> authorities) {
        if (authorities == null) {
            return null;
        }
        for (String authority : authorities) {
            try {
                return RolUsuario.valueOf(authority);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private void validarRolModulo(RolUsuario rol) {
        if (!(rol == RolUsuario.ROL_SUPER_ADMIN
                || rol == RolUsuario.ROL_COMPRADOR
                || rol == RolUsuario.ROL_JEFE_ALMACENES)) {
            throw new CustomBusinessException(ApiErrorCode.ROL_INSUFICIENTE, "ROL_SIN_PERMISO_ESTADO");
        }
    }


    public TipoOrdenCompra determinarTipoOrdenPorDetalles(List<Producto> productos) {
        if (productos == null || productos.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Debe registrar al menos un producto para determinar el tipo de OC");
        }

        boolean todosSinControl = productos.stream()
                .allMatch(p -> p != null && p.getModoControlInventario() == ModoControlInventario.SIN_CONTROL_STOCK);

        boolean todosConControl = productos.stream()
                .allMatch(p -> p != null && p.getModoControlInventario() == ModoControlInventario.CONTROL_STOCK);

        if (todosSinControl) {
            return TipoOrdenCompra.SERVICIOS;
        }

        if (todosConControl) {
            return TipoOrdenCompra.BIENES;
        }

        throw new CustomBusinessException(ApiErrorCode.OC_TIPO_MEZCLADO_NO_PERMITIDO,
                "No se permite mezclar productos con y sin control de inventario en una misma OC");
    }

    public HistorialEstadoOrden ejecutarServicio(Long ordenId,
                                                 CustomUserDetails principal,
                                                 String observaciones) {
        if (principal == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "USUARIO_NO_AUTENTICADO");
        }

        OrdenCompra orden = ordenCompraRepository.findByIdWithDetalles(ordenId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "ORDEN_NO_ENCONTRADA"));

        if (orden.getTipo() != TipoOrdenCompra.SERVICIOS) {
            throw new CustomBusinessException(ApiErrorCode.OC_SERVICIO_INVALIDA, "OC_NO_ES_DE_SERVICIOS");
        }

        if (!(orden.getEstado() == EstadoOrdenCompra.ENVIADA || orden.getEstado() == EstadoOrdenCompra.PARCIALMENTE_RECIBIDA)) {
            throw new CustomBusinessException(ApiErrorCode.OC_TRANSICION_INVALIDA, "ESTADO_NO_PERMITE_EJECUTAR_SERVICIO");
        }

        if (orden.getDetalles() != null) {
            for (OrdenCompraDetalle detalle : orden.getDetalles()) {
                if (detalle != null) {
                    detalle.setCantidadRecibida(detalle.getCantidad());
                }
            }
        }

        orden.setEstado(EstadoOrdenCompra.RECIBIDA_COMPLETAMENTE);
        ordenCompraRepository.save(orden);

        Usuario usuario = principal.getUsuario() != null ? principal.getUsuario() : new Usuario();
        if (usuario.getId() == null) {
            usuario.setId(principal.getId());
        }

        HistorialEstadoOrden historial = HistorialEstadoOrden.builder()
                .ordenCompra(orden)
                .estado(EstadoOrdenCompra.RECIBIDA_COMPLETAMENTE)
                .fechaCambio(LocalDateTime.now())
                .cambiadoPor(usuario)
                .observaciones(observaciones)
                .build();

        return historialEstadoOrdenRepository.save(historial);
    }

}
