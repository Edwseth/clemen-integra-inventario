package com.willyes.clemenintegra.planeacion.service;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.planeacion.dto.CorridaMrpResponseDTO;
import com.willyes.clemenintegra.planeacion.dto.MrpSimpleRequestDTO;
import com.willyes.clemenintegra.planeacion.dto.SugerenciaAbastecimientoResponseDTO;
import com.willyes.clemenintegra.planeacion.mapper.PlaneacionMrpMapper;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.OrigenSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.TipoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import com.willyes.clemenintegra.planeacion.repository.SugerenciaAbastecimientoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaneacionMrpServiceImpl implements PlaneacionMrpService {

    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    private final CorridaMrpRepository corridaMrpRepository;
    private final SugerenciaAbastecimientoRepository sugerenciaAbastecimientoRepository;
    private final PlaneacionMrpMapper planeacionMrpMapper;

    @Override
    @Transactional
    public CorridaMrpResponseDTO ejecutarMrpSimple(MrpSimpleRequestDTO request, Long usuarioId) {
        MrpSimpleRequestDTO safeRequest = request != null ? request : new MrpSimpleRequestDTO();
        List<TipoCategoria> tiposCategorias = parseTiposCategorias(safeRequest.getCategoriasProducto());

        boolean soloControlStock = safeRequest.getSoloControlStock() == null || Boolean.TRUE.equals(safeRequest.getSoloControlStock());

        List<Producto> candidatos;
        if (soloControlStock) {
            if (tiposCategorias.isEmpty()) {
                candidatos = productoRepository.findByModoControlInventarioAndActivoTrue(ModoControlInventario.CONTROL_STOCK);
            } else {
                candidatos = productoRepository.findByModoControlInventarioAndActivoTrueAndCategoriaProducto_TipoIn(
                        ModoControlInventario.CONTROL_STOCK,
                        tiposCategorias
                );
            }
        } else {
            candidatos = productoRepository.findAll(buildProductoSpecification(safeRequest, tiposCategorias));
        }
        log.info("Iniciando corrida MRP simple para {} productos candidatos", candidatos.size());

        List<SugerenciaAbastecimiento> sugerencias = new ArrayList<>();
        LocalDate fechaRequerida = safeRequest.getHorizonteDesde() != null ? safeRequest.getHorizonteDesde() : LocalDate.now();

        for (Producto producto : candidatos) {
            BigDecimal stockActual = obtenerStockActual(producto.getId().longValue());
            BigDecimal pendienteRecepcion = obtenerPendienteRecepcion(producto.getId().longValue());
            BigDecimal stockProyectado = stockActual.add(pendienteRecepcion);

            BigDecimal stockSeguridad = producto.getStockSeguridad() != null ? producto.getStockSeguridad() : BigDecimal.ZERO;
            BigDecimal stockMinimo = producto.getStockMinimo() != null ? producto.getStockMinimo() : BigDecimal.ZERO;
            BigDecimal puntoAccion = stockMinimo.add(stockSeguridad);

            if (stockProyectado.compareTo(puntoAccion) >= 0) {
                continue;
            }

            BigDecimal cantidadSugerida = calcularCantidadSugerida(producto.getStockMaximoPlaneacion(), puntoAccion, stockProyectado);
            LocalDate fechaSugeridaPedido = calcularFechaSugeridaPedido(producto.getLeadTimeCompraDias(), fechaRequerida);

            SugerenciaAbastecimiento sugerencia = SugerenciaAbastecimiento.builder()
                    .producto(producto)
                    .tipo(TipoSugerenciaAbastecimiento.COMPRA)
                    .cantidadSugerida(cantidadSugerida)
                    .fechaRequerida(fechaRequerida)
                    // Si no hay lead time de compra, proponemos la misma fecha requerida
                    .fechaSugeridaPedido(fechaSugeridaPedido)
                    .estado(EstadoSugerenciaAbastecimiento.PENDIENTE)
                    .origen(OrigenSugerenciaAbastecimiento.MRP_SIMPLE)
                    .observaciones(String.format("stockProyectado %s < puntoAccion %s", stockProyectado, puntoAccion))
                    .build();

            sugerencias.add(sugerencia);
        }

        CorridaMrp corrida = CorridaMrp.builder()
                .fechaEjecucion(LocalDateTime.now())
                .horizonteDesde(safeRequest.getHorizonteDesde())
                .horizonteHasta(safeRequest.getHorizonteHasta())
                .usuarioEjecutoId(usuarioId)
                .resumen(String.format("Sugerencias generadas: %d", sugerencias.size()))
                .build();

        sugerencias.forEach(s -> s.setCorrida(corrida));
        corrida.setSugerencias(sugerencias);

        CorridaMrp persistida = corridaMrpRepository.save(corrida);
        return planeacionMrpMapper.toDto(persistida);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CorridaMrpResponseDTO> listarCorridas(LocalDate fechaDesde, LocalDate fechaHasta, String modo, Pageable pageable) {
        if (modo != null && !"MRP_SIMPLE".equalsIgnoreCase(modo)) {
            return Page.empty(pageable);
        }

        Specification<CorridaMrp> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaEjecucion"), fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaEjecucion"), fechaHasta.plusDays(1).atStartOfDay().minusSeconds(1)));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return corridaMrpRepository.findAll(spec, pageable).map(planeacionMrpMapper::toDtoSinSugerencias);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SugerenciaAbastecimientoResponseDTO> listarSugerencias(Long corridaId, Long productoId, String estado, String tipo, Pageable pageable) {
        Specification<SugerenciaAbastecimiento> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (corridaId != null) {
                predicates.add(cb.equal(root.join("corrida", JoinType.INNER).get("id"), corridaId));
            }
            if (productoId != null) {
                predicates.add(cb.equal(root.join("producto", JoinType.INNER).get("id"), productoId));
            }

            EstadoSugerenciaAbastecimiento estadoEnum = parseEstado(estado);
            if (estadoEnum != null) {
                predicates.add(cb.equal(root.get("estado"), estadoEnum));
            }

            TipoSugerenciaAbastecimiento tipoEnum = parseTipo(tipo);
            if (tipoEnum != null) {
                predicates.add(cb.equal(root.get("tipo"), tipoEnum));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return sugerenciaAbastecimientoRepository.findAll(spec, pageable).map(planeacionMrpMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SugerenciaAbastecimientoResponseDTO> listarSugerenciasPorCorrida(Long corridaId, Pageable pageable) {
        return listarSugerencias(corridaId, null, null, null, pageable);
    }

    private Specification<Producto> buildProductoSpecification(MrpSimpleRequestDTO request, List<TipoCategoria> tiposCategorias) {
        MrpSimpleRequestDTO safeRequest = request != null ? request : new MrpSimpleRequestDTO();
        return (root, query, cb) -> {
            query.distinct(true);
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            boolean soloControlStock = safeRequest.getSoloControlStock() == null || Boolean.TRUE.equals(safeRequest.getSoloControlStock());
            if (soloControlStock) {
                predicates.add(cb.equal(root.get("modoControlInventario"), ModoControlInventario.CONTROL_STOCK));
            }
            predicates.add(cb.isTrue(root.get("activo")));

            if (tiposCategorias != null && !tiposCategorias.isEmpty()) {
                predicates.add(root.join("categoriaProducto").get("tipo").in(tiposCategorias));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private List<TipoCategoria> parseTiposCategorias(List<String> categoriasProducto) {
        if (categoriasProducto == null || categoriasProducto.isEmpty()) {
            return Collections.emptyList();
        }

        return categoriasProducto.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(categoria -> {
                    try {
                        return TipoCategoria.valueOf(categoria);
                    } catch (IllegalArgumentException ex) {
                        log.warn("Tipo de categoría inválido recibido: {}", categoria);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private BigDecimal obtenerStockActual(Long productoId) {
        List<Object[]> resultados = loteProductoRepository.sumarPorEstado(productoId);
        Map<EstadoLote, BigDecimal> porEstado = new EnumMap<>(EstadoLote.class);
        for (Object[] fila : resultados) {
            if (fila.length >= 2 && fila[0] instanceof EstadoLote estado) {
                BigDecimal total = fila[1] != null ? (BigDecimal) fila[1] : BigDecimal.ZERO;
                porEstado.put(estado, total);
            }
        }
        BigDecimal disponible = porEstado.getOrDefault(EstadoLote.DISPONIBLE, BigDecimal.ZERO);
        BigDecimal liberado = porEstado.getOrDefault(EstadoLote.LIBERADO, BigDecimal.ZERO);
        return disponible.add(liberado);
    }

    private BigDecimal obtenerPendienteRecepcion(Long productoId) {
        List<EstadoOrdenCompra> estados = List.of(
                EstadoOrdenCompra.CREADA,
                EstadoOrdenCompra.ENVIADA,
                EstadoOrdenCompra.PARCIALMENTE_RECIBIDA
        );
        BigDecimal pendiente = ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstados(productoId, estados);
        return pendiente != null ? pendiente : BigDecimal.ZERO;
    }

    private BigDecimal calcularCantidadSugerida(BigDecimal stockMaximoPlaneacion, BigDecimal puntoAccion, BigDecimal stockProyectado) {
        BigDecimal cantidad;
        if (stockMaximoPlaneacion != null) {
            cantidad = stockMaximoPlaneacion.subtract(stockProyectado);
            if (cantidad.compareTo(BigDecimal.ZERO) < 0) {
                cantidad = BigDecimal.ZERO;
            }
        } else {
            cantidad = puntoAccion.subtract(stockProyectado);
        }
        return cantidad.setScale(6, RoundingMode.HALF_UP);
    }

    private LocalDate calcularFechaSugeridaPedido(Integer leadTimeCompraDias, LocalDate fechaRequerida) {
        if (leadTimeCompraDias != null && leadTimeCompraDias > 0) {
            return fechaRequerida.minusDays(leadTimeCompraDias);
        }
        // Sin lead time explícito, usamos la misma fecha requerida para simplificar la sugerencia.
        return fechaRequerida;
    }

    private EstadoSugerenciaAbastecimiento parseEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoSugerenciaAbastecimiento.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Estado de sugerencia inválido recibido: {}", estado);
            return null;
        }
    }

    private TipoSugerenciaAbastecimiento parseTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        try {
            return TipoSugerenciaAbastecimiento.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Tipo de sugerencia inválido recibido: {}", tipo);
            return null;
        }
    }
}
