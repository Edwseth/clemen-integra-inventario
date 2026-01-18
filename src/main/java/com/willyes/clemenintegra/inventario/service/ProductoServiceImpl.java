package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.mapper.ProductoMapper;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.calidad.repository.PlantillaAnalisisMicrobiologicoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.willyes.clemenintegra.inventario.service.spec.ProductoSpecifications.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ProductoMapper productoMapper;
    private final StockQueryService stockQueryService;
    private final PlantillaAnalisisMicrobiologicoRepository plantillaAnalisisMicrobiologicoRepository;

    private boolean esProductoFabricable(com.willyes.clemenintegra.inventario.model.CategoriaProducto categoria, String sku) {
        if (categoria == null || categoria.getTipo() == null || sku == null) {
            return false;
        }
        String skuNormalizado = sku.trim().toUpperCase();
        return switch (categoria.getTipo()) {
            case PRODUCTO_TERMINADO -> skuNormalizado.startsWith("PT");
            case PRODUCTO_SEMI_ELABORADO -> skuNormalizado.startsWith("PS");
            default -> false;
        };
    }

    private BigDecimal sanitizeRendimiento(BigDecimal val) {
        if (val == null) return null;
        if (val.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rendimiento × Unidad debe ser >= 0.00");
        }
        return val.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolverRendimientoUnidad(ProductoRequestDTO dto, com.willyes.clemenintegra.inventario.model.CategoriaProducto categoria) {
        if (!esProductoFabricable(categoria, dto.getSku())) {
            return null;
        }

        BigDecimal rendimiento = sanitizeRendimiento(dto.getRendimientoUnidad());
        if (rendimiento == null || rendimiento.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El rendimiento por unidad es obligatorio y debe ser mayor que cero para productos terminados y semielaborados.");
        }
        return rendimiento;
    }

    private String sanitizeBusqueda(String term) {
        return term == null ? "" : term.trim();
    }

    private BigDecimal normalizeToScale6(BigDecimal value) {
        return value != null ? value.setScale(6, RoundingMode.HALF_UP) : null;
    }

    private com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico obtenerPlantillaMicro(Long plantillaId) {
        if (plantillaId == null) {
            return null;
        }
        return plantillaAnalisisMicrobiologicoRepository.findById(plantillaId)
                .orElseThrow(() -> new IllegalArgumentException("Plantilla microbiológica no encontrada con ID: " + plantillaId));
    }

    private ProductoResumenDTO mapToResumenDto(Producto producto) {
        if (producto == null) {
            return null;
        }
        return ProductoResumenDTO.builder()
                .id(producto.getId() != null ? producto.getId().longValue() : null)
                .nombre(producto.getNombre())
                .codigoSku(producto.getCodigoSku())
                .tipoCategoria(producto.getCategoriaProducto() != null
                        ? producto.getCategoriaProducto().getTipo() : null)
                .unidadMedida(producto.getUnidadMedida() != null
                        ? producto.getUnidadMedida().getNombre() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoResponseDTO> listarTodos(String nombre, String sku, Long categoriaProductoId, Boolean activo, Pageable pageable) {

        log.debug("Listando productos con filtros nombre={}, sku={}, categoriaId={}, activo={}",
                nombre, sku, categoriaProductoId, activo);

        Specification<Producto> spec = Specification
                .where(nombreContains(nombre))
                .and(skuContains(sku))
                .and(categoriaProductoIdEquals(categoriaProductoId))
                .and(activoEquals(activo));

        Page<Producto> productos = productoRepository.findAll(spec, pageable);
        log.debug("Total de productos devueltos: {}", productos.getTotalElements());

        List<Long> ids = productos.getContent().stream()
                .map(p -> p.getId().longValue())
                .toList();
        Map<Long, BigDecimal> stockMap = stockQueryService.obtenerStockDisponible(ids);

        productos.forEach(p -> {
            if (p == null) {
                log.warn("Producto nulo detectado durante listado");
            } else {
                log.debug("Producto cargado en listado: id={} nombre={}", p.getId(), p.getNombre());
            }
        });

        return productos.map(p -> buildDto(p, stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO)));
    }

    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> buscarPorCategoria(String categoria) {
        TipoCategoria tipo = TipoCategoria.valueOf(categoria.toUpperCase());
        List<Producto> lista = productoRepository.findByCategoriaProducto_Tipo(tipo);
        Map<Long, BigDecimal> stockMap = stockQueryService.obtenerStockDisponible(
                lista.stream().map(p -> p.getId().longValue()).toList());
        return lista.stream()
                .map(p -> buildDto(p, stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> findByCategoriaTipo(String tipo) {
        TipoCategoria tipoEnum = TipoCategoria.valueOf(tipo); // conversión aquí
        List<Producto> lista = Optional.ofNullable(productoRepository.findByCategoriaProducto_Tipo(tipoEnum))
                .orElse(Collections.emptyList());
        Map<Long, BigDecimal> stockMap = stockQueryService.obtenerStockDisponible(
                lista.stream().map(p -> p.getId().longValue()).toList());
        return lista.stream()
                .map(p -> buildDto(p, stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> findByCategoriaTipoIn(List<String> tipos) {
        List<TipoCategoria> tiposEnum = tipos.stream()
                .map(TipoCategoria::valueOf)
                .toList();
        List<Producto> lista = productoRepository.findByCategoriaProducto_TipoIn(tiposEnum);
        Map<Long, BigDecimal> stockMap = stockQueryService.obtenerStockDisponible(
                lista.stream().map(p -> p.getId().longValue()).toList());
        return lista.stream()
                .map(p -> buildDto(p, stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> findProductosFabricables() {
        List<TipoCategoria> tiposFabricables = List.of(
                TipoCategoria.PRODUCTO_TERMINADO,
                TipoCategoria.PRODUCTO_SEMI_ELABORADO
        );

        List<Producto> lista = Optional.ofNullable(productoRepository.findByCategoriaProducto_TipoIn(tiposFabricables))
                .orElse(Collections.emptyList())
                .stream()
                .filter(p -> p.getCategoriaProducto() != null && tiposFabricables.contains(p.getCategoriaProducto().getTipo()))
                .toList();

        Map<Long, BigDecimal> stockMap = stockQueryService.obtenerStockDisponible(
                lista.stream().map(p -> p.getId().longValue()).toList());

        return lista.stream()
                .map(p -> buildDto(p, stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO)))
                .toList();
    }

    @Override
    @Transactional
    public ProductoResponseDTO crearProducto(ProductoRequestDTO dto, Long usuarioId) {
        validarDuplicados(dto.getSku(), dto.getNombre());

        var unidad = unidadMedidaRepository.findById(dto.getUnidadMedidaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada"));

        var categoria = categoriaProductoRepository.findById(dto.getCategoriaProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Producto producto = Producto.builder()
                .codigoSku(dto.getSku())
                .nombre(dto.getNombre())
                .descripcionProducto(dto.getDescripcionProducto())
                .stockMinimo(dto.getStockMinimo())
                .stockMinimoProveedor(dto.getStockMinimoProveedor())
                .leadTimeCompraDias(dto.getLeadTimeCompraDias())
                .leadTimeProduccionDias(dto.getLeadTimeProduccionDias())
                .stockSeguridad(normalizeToScale6(dto.getStockSeguridad()))
                .stockMaximoPlaneacion(normalizeToScale6(dto.getStockMaximoPlaneacion()))
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .plantillaAnalisisMicrobiologico(obtenerPlantillaMicro(dto.getPlantillaAnalisisMicroId()))
                .creadoPor(usuario)
                .build();

        aplicarBanderasCalidadDesdeDto(producto, dto);

        producto.setRendimientoUnidad(resolverRendimientoUnidad(dto, categoria));

        productoRepository.save(producto);
        return buildDto(producto);
    }

    @Override
    @Transactional
    public ProductoResponseDTO actualizarProducto(Long id, ProductoRequestDTO dto, Long usuarioId) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        // Validar duplicados de SKU y nombre excluyendo el mismo producto actual
        validarDuplicadosAlActualizar(id, dto.getSku(), dto.getNombre());

        var unidad = unidadMedidaRepository.findById(dto.getUnidadMedidaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada"));

        var categoria = categoriaProductoRepository.findById(dto.getCategoriaProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Actualizar campos
        producto.setCodigoSku(dto.getSku());
        producto.setNombre(dto.getNombre());
        producto.setDescripcionProducto(dto.getDescripcionProducto());
        producto.setStockMinimo(dto.getStockMinimo());
        producto.setStockMinimoProveedor(dto.getStockMinimoProveedor());
        producto.setLeadTimeCompraDias(dto.getLeadTimeCompraDias());
        producto.setLeadTimeProduccionDias(dto.getLeadTimeProduccionDias());
        producto.setStockSeguridad(normalizeToScale6(dto.getStockSeguridad()));
        producto.setStockMaximoPlaneacion(normalizeToScale6(dto.getStockMaximoPlaneacion()));
        producto.setUnidadMedida(unidad);
        producto.setCategoriaProducto(categoria);
        producto.setCreadoPor(usuario);
        producto.setPlantillaAnalisisMicrobiologico(obtenerPlantillaMicro(dto.getPlantillaAnalisisMicroId()));

        aplicarBanderasCalidadDesdeDto(producto, dto);

        producto.setRendimientoUnidad(resolverRendimientoUnidad(dto, categoria));

        productoRepository.save(producto);
        BigDecimal stock = stockQueryService.obtenerStockDisponible(producto.getId().longValue());
        return buildDto(producto, stock);
    }

    @Override
    @Transactional
    public ProductoResponseDTO actualizarCamposCalidad(Long id, ProductoCalidadUpdateDTO dto) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Producto no encontrado con ID: " + id));

        producto.setRequiereAnalisisFisico(dto.getRequiereAnalisisFisico());
        producto.setRequiereAnalisisQuimico(dto.getRequiereAnalisisQuimico());
        producto.setRequiereAnalisisMicrobiologico(dto.getRequiereAnalisisMicrobiologico());
        producto.recomputarTipoAnalisisDesdeBanderas();

        productoRepository.save(producto);
        BigDecimal stock = stockQueryService.obtenerStockDisponible(producto.getId().longValue());
        return buildDto(producto, stock);
    }

    @Transactional
    public void eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        boolean hasLotes = loteProductoRepository.existsByProducto(producto);
        boolean hasMovimientos = movimientoInventarioRepository.existsByProductoId(id);
        if (hasLotes || hasMovimientos) {
            throw new IllegalStateException("El producto tiene dependencias y no puede eliminarse");
        }
        productoRepository.delete(producto);
    }

    @Transactional
    public ProductoResponseDTO actualizarEstado(Long id, Boolean activo) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        producto.setActivo(Boolean.TRUE.equals(activo));
        productoRepository.save(producto);
        BigDecimal stock = stockQueryService.obtenerStockDisponible(producto.getId().longValue());
        return buildDto(producto, stock);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        BigDecimal stock = stockQueryService.obtenerStockDisponible(id);
        return buildDto(producto, stock);
    }

    @Transactional
    public UnidadMedidaResponseDTO cambiarUnidadMedida(Long productoId, UnidadMedidaRequestDTO dto) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));

        boolean tieneMovimientos = movimientoInventarioRepository.existsByProductoId(productoId);
        if (tieneMovimientos) {
            throw new IllegalStateException("No se puede modificar la unidad de medida: existen movimientos asociados");
        }

        UnidadMedida unidad = unidadMedidaRepository.findByNombre(dto.getNombre())
                .orElseGet(() -> {
                    UnidadMedida u = new UnidadMedida();
                    u.setNombre(dto.getNombre().trim());
                    u.setSimbolo(dto.getSimbolo().trim());
                    return unidadMedidaRepository.save(u);
                });

        producto.setUnidadMedida(unidad);
        productoRepository.save(producto);

        return new UnidadMedidaResponseDTO(unidad.getId(), unidad.getNombre(), unidad.getSimbolo());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoOptionDTO> buscarOpciones(String q, Boolean activo, Long almacenId, Pageable pageable) {
        // Mantén tu “safe sort”
        Sort sort = pageable.getSort();
        Sort safe = Sort.by(sort.stream()
                .map(o -> switch (o.getProperty()) {
                    case "nombre", "codigoSku", "sku", "id" ->
                            "sku".equals(o.getProperty()) ? new Sort.Order(o.getDirection(), "codigoSku") : o;
                    default -> new Sort.Order(o.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC, "nombre");
                }).toList());
        Pageable safePage = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), safe);

        String term = q == null ? null : q.trim();
        Page<Producto> page = almacenId == null
                ? productoRepository.buscarPorTexto(term, activo, safePage)
                : productoRepository.buscarParaConteo(term, almacenId, safePage);

        // ✅ Ahora mapeamos también la unidad
        return page.map(p -> {
            ProductoOptionDTO.UnidadMiniDTO unidadDTO = null;
            if (p.getUnidadMedida() != null) {
                String simbolo = p.getUnidadMedida().getSimbolo();
                String simboloImp;
                String nombre = p.getUnidadMedida().getNombre();
                String nombrePlural;
                try {
                    // si tu entidad ya tiene getSimboloImpresion()
                    simboloImp = p.getUnidadMedida().getSimboloImpresion();
                } catch (Throwable t) {
                    // tolerante si aún no existe el campo en el modelo
                    simboloImp = null;
                }
                try {
                    nombrePlural = p.getUnidadMedida().getNombrePlural();
                } catch (Throwable t) {
                    nombrePlural = null; // deja que el FE haga fallback a nombre+"S"
                }
                unidadDTO = ProductoOptionDTO.UnidadMiniDTO.builder()
                        .nombre(nombre)
                        .nombrePlural(nombrePlural)
                        .simbolo(simbolo)
                        .simboloImpresion((simboloImp != null && !simboloImp.isBlank()) ? simboloImp : simbolo)
                        .build();
            }

            return ProductoOptionDTO.builder()
                    .id(p.getId() == null ? null : Long.valueOf(p.getId()))
                    .nombre(p.getNombre())
                    .sku(p.getCodigoSku())
                    .unidad(unidadDTO) // ✅ se envía al frontend
                    .build();
        });
    }

    @Override
    public Producto findById(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public Workbook generarReporteStockDisponibleExcel() {
        List<Producto> productos = productoRepository.findAll();
        Map<Long, BigDecimal> stockMap = stockQueryService.obtenerStockDisponible(
                productos.stream().map(p -> p.getId().longValue()).toList());

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Stock Disponible");
        CreationHelper creationHelper = workbook.getCreationHelper();
        DataFormat dataFormat = creationHelper.createDataFormat();
        CellStyle numericStyle = workbook.createCellStyle();
        numericStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

        Row header = sheet.createRow(0);
        String[] columnas = {
                "ID", "Código SKU", "Nombre", "Stock Disponible",
                "Unidad de Medida", "Stock Mínimo", "Activo", "Categoría"
        };
        for (int i = 0; i < columnas.length; i++) {
            header.createCell(i).setCellValue(columnas[i]);
        }

        int rowNum = 1;
        for (Producto producto : productos) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(producto.getId());
            row.createCell(1).setCellValue(producto.getCodigoSku());
            row.createCell(2).setCellValue(producto.getNombre());
            BigDecimal stock = stockMap.getOrDefault(producto.getId().longValue(), BigDecimal.ZERO);
            Cell stockCell = row.createCell(3);
            stockCell.setCellValue(stock != null ? stock.doubleValue() : 0d);
            stockCell.setCellStyle(numericStyle);
            row.createCell(4).setCellValue(producto.getUnidadMedida() != null ? producto.getUnidadMedida().getNombre() : "");
            BigDecimal stockMinimo = producto.getStockMinimo() != null ? producto.getStockMinimo() : BigDecimal.ZERO;
            Cell minimoCell = row.createCell(5);
            minimoCell.setCellValue(stockMinimo.doubleValue());
            minimoCell.setCellStyle(numericStyle);
            row.createCell(6).setCellValue(producto.isActivo());
            row.createCell(7).setCellValue(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getNombre() : "");
        }

        for (int i = 0; i < columnas.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    private void validarDuplicados(String sku, String nombre) {
        if (productoRepository.existsByCodigoSku(sku)) {
            throw new DataIntegrityViolationException("Ya existe un producto con ese código SKU.");
        }
        if (productoRepository.existsByNombre(nombre)) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre.");
        }
    }

    private void validarDuplicadosAlActualizar(Long id, String sku, String nombre) {
        if (productoRepository.existsByCodigoSkuAndIdNot(sku, id)) {
            throw new DataIntegrityViolationException("Ya existe otro producto con ese código SKU.");
        }
        if (productoRepository.existsByNombreAndIdNot(nombre, id)) {
            throw new IllegalArgumentException("Ya existe otro producto con ese nombre.");
        }
    }

    private ProductoResponseDTO buildDto(Producto producto, BigDecimal stockDisponible) {
        ProductoResponseDTO dto = productoMapper.toDto(producto);
        dto.setEditable(producto.isActivo());
        boolean hasLotes = loteProductoRepository.existsByProducto(producto);
        boolean hasMovimientos = movimientoInventarioRepository.existsByProductoId(producto.getId().longValue());
        dto.setEliminable(!hasLotes && !hasMovimientos);
        dto.setInactivable(true);
        dto.setStockDisponible(stockDisponible);
        return dto;
    }

    private ProductoResponseDTO buildDto(Producto producto) {
        BigDecimal stock = stockQueryService.obtenerStockDisponible(producto.getId().longValue());
        return buildDto(producto, stock);
    }

    private void aplicarBanderasCalidadDesdeDto(Producto producto, ProductoRequestDTO dto) {
        boolean flagsPresentes = dto.getRequiereAnalisisFisico() != null
                || dto.getRequiereAnalisisQuimico() != null
                || dto.getRequiereAnalisisMicrobiologico() != null;

        boolean requiereFisico = Boolean.TRUE.equals(dto.getRequiereAnalisisFisico());
        boolean requiereQuimico = Boolean.TRUE.equals(dto.getRequiereAnalisisQuimico());
        boolean requiereMicro = Boolean.TRUE.equals(dto.getRequiereAnalisisMicrobiologico());

        if (!flagsPresentes) {
            // LEGACY: compatibilidad con clientes que envían solo tipoAnalisisCalidad
            TipoAnalisisCalidad legado = productoMapper.mapTipoAnalisisCalidad(dto.getTipoAnalisisCalidad());
            requiereFisico = legado == TipoAnalisisCalidad.FISICO || legado == TipoAnalisisCalidad.AMBOS;
            requiereQuimico = legado == TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO || legado == TipoAnalisisCalidad.AMBOS;
            requiereMicro = legado == TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO || legado == TipoAnalisisCalidad.AMBOS;
        }

        producto.setRequiereAnalisisFisico(requiereFisico);
        producto.setRequiereAnalisisQuimico(requiereQuimico);
        producto.setRequiereAnalisisMicrobiologico(requiereMicro);
        producto.recomputarTipoAnalisisDesdeBanderas();
    }

    public List<ProductoConEstadoLoteDTO> buscarProductosConLotesPorEstado(String estado) {
        EstadoLote estadoEnum = EstadoLote.valueOf(estado.toUpperCase());

        List<LoteProducto> lotes = loteProductoRepository.findByEstado(estadoEnum);

        return lotes.stream()
                .filter(l -> l.getProducto() != null)
                .map(lote -> {
                    Producto producto = lote.getProducto();
                    return new ProductoConEstadoLoteDTO(
                            producto.getId().longValue(),
                            producto.getCodigoSku(),
                            producto.getNombre(),
                            lote.getEstado().name()
                    );
                })
                .distinct() // evitar duplicados si hay múltiples lotes de un mismo producto
                .toList();
    }

    public List<ProductoConLotesDTO> buscarProductosConLotesAgrupadosPorEstado(String estado) {
        EstadoLote estadoEnum = EstadoLote.valueOf(estado.toUpperCase());

        List<LoteProducto> lotes = loteProductoRepository.findByEstado(estadoEnum);

        // Agrupar lotes por producto
        Map<Producto, List<LoteProducto>> agrupados = lotes.stream()
                .filter(l -> l.getProducto() != null)
                .collect(Collectors.groupingBy(LoteProducto::getProducto));

        // Eliminar posibles agrupaciones con clave nula
        agrupados.remove(null);

        // Convertir a DTOs
        return agrupados.entrySet().stream()
                .map(entry -> {
                    Producto producto = entry.getKey();
                    List<LoteSimpleDTO> loteDTOs = entry.getValue().stream().map(l -> {
                        return new LoteSimpleDTO(
                                l.getId(),
                                l.getCodigoLote(),
                                l.getEstado().name(),
                                l.getFechaFabricacion(),
                                l.getFechaVencimiento()
                        );
                    }).toList();

                    return new ProductoConLotesDTO(
                            producto.getId().longValue(),
                            producto.getCodigoSku(),
                            producto.getNombre(),
                            loteDTOs
                    );
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoResumenDTO> buscarInsumos(String term, Pageable pageable) {
        String sanitized = sanitizeBusqueda(term);
        Page<Producto> page = productoRepository.searchInsumosByNombre(sanitized, pageable);
        return page.map(this::mapToResumenDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoResumenDTO> buscarProductosTerminados(String term, Pageable pageable) {
        String sanitized = sanitizeBusqueda(term);
        Page<Producto> page = productoRepository.searchProductosTerminadosByNombre(sanitized, pageable);
        return page.map(this::mapToResumenDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Producto> buscarProductosFabricablesAutocomplete(String term, Pageable pageable) {
        if (term == null || term.trim().isEmpty()) {
            return Page.empty(pageable);
        }

        String cleaned = term.trim();

        List<TipoCategoria> tiposFabricables = List.of(
                TipoCategoria.PRODUCTO_TERMINADO,
                TipoCategoria.PRODUCTO_SEMI_ELABORADO
        );

        return productoRepository.buscarFabricablesAutocomplete(tiposFabricables, cleaned, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InsumoAutocompleteDTO> buscarInsumosAutocomplete(String term, Pageable pageable) {
        if (term == null || term.trim().isEmpty()) {
            return Page.empty(pageable);
        }

        List<TipoCategoria> tipos = List.of(
                TipoCategoria.MATERIA_PRIMA,
                TipoCategoria.MATERIAL_EMPAQUE,
                TipoCategoria.SUMINISTROS,
                TipoCategoria.PRODUCTO_SEMI_ELABORADO
        );
        return productoRepository.buscarInsumosAutocomplete(tipos, term.trim(), pageable);
    }

}
