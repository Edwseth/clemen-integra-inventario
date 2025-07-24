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
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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
    private final JwtTokenService jwtTokenService;

    private Long obtenerUsuarioIdDesdeToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new IllegalStateException("No hay autenticación en el contexto");
        }

        if (authentication.getCredentials() instanceof String token) {
            Claims claims = jwtTokenService.extraerClaims(token);
            return claims.get("usuarioId", Long.class);
        }

        throw new IllegalStateException("No se pudo extraer el token JWT");
    }

    public Page<ProductoResponseDTO> listarTodos(Pageable pageable) {

        System.out.println("🟢 Entró correctamente a ProductoServiceImpl.listarTodos()");
        Page<Producto> productos = productoRepository.findAll(pageable);
        System.out.println("▶️ Total productos devueltos: " + productos.getTotalElements());

        productos.forEach(p -> {
            if (p == null) {
                System.out.println("⚠️ Producto nulo detectado");
            } else {
                System.out.println("📦 Producto cargado: " + p.getId() + " - " + p.getNombre());
            }
        });
        return productos.map(productoMapper::safeToDto);
    }

    public List<ProductoResponseDTO> buscarPorCategoria(String categoria) {
        TipoCategoria tipo = TipoCategoria.valueOf(categoria.toUpperCase());
        return productoRepository.findByCategoriaProducto_Tipo(tipo)
                .stream()
                .map(ProductoResponseDTO::new)
                .collect(Collectors.toList());
    }

    public ProductoResponseDTO crearProducto(ProductoRequestDTO dto) {
        validarDuplicados(dto.getCodigoSku(), dto.getNombre());

        var unidad = unidadMedidaRepository.findById(dto.getUnidadMedidaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada"));

        var categoria = categoriaProductoRepository.findById(dto.getCategoriaProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        Long usuarioId = obtenerUsuarioIdDesdeToken();

        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Producto producto = Producto.builder()
                .codigoSku(dto.getCodigoSku())
                .nombre(dto.getNombre())
                .descripcionProducto(dto.getDescripcionProducto())
                .stockActual(BigDecimal.ZERO)
                .stockMinimo(dto.getStockMinimo())
                .stockMinimoProveedor(dto.getStockMinimoProveedor())
                .activo(true)
                .tipoAnalisis(obtenerTipoAnalisisDesdeDto(dto.getTipoAnalisisCalidad()))
                .fechaCreacion(LocalDateTime.now())
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .build();

        productoRepository.save(producto);
        return productoMapper.toDto(producto);
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

    // Mapeo delegado a MapStruct
    public ProductoResponseDTO obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        return productoMapper.toDto(producto);
    }

    public ProductoResponseDTO actualizarProducto(Long id, ProductoRequestDTO dto) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        // Validar duplicados de SKU y nombre excluyendo el mismo producto actual
        validarDuplicadosAlActualizar(id, dto.getCodigoSku(), dto.getNombre());

        var unidad = unidadMedidaRepository.findById(dto.getUnidadMedidaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada"));

        var categoria = categoriaProductoRepository.findById(dto.getCategoriaProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        Long usuarioId = obtenerUsuarioIdDesdeToken();
        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Actualizar campos
        producto.setCodigoSku(dto.getCodigoSku());
        producto.setNombre(dto.getNombre());
        producto.setDescripcionProducto(dto.getDescripcionProducto());
        producto.setStockMinimo(dto.getStockMinimo());
        producto.setStockMinimoProveedor(dto.getStockMinimoProveedor());
        producto.setTipoAnalisis(obtenerTipoAnalisisDesdeDto(dto.getTipoAnalisisCalidad()));
        producto.setUnidadMedida(unidad);
        producto.setCategoriaProducto(categoria);
        producto.setCreadoPor(usuario);

        productoRepository.save(producto);
        return productoMapper.toDto(producto);
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
                .orElseGet(() -> unidadMedidaRepository.save(new UnidadMedida(null, dto.getNombre(), dto.getSimbolo())));

        producto.setUnidadMedida(unidad);
        productoRepository.save(producto);

        return new UnidadMedidaResponseDTO(unidad.getId(), unidad.getNombre(), unidad.getSimbolo());
    }

    public void eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        // Aquí debes validar relaciones activas (ejemplo con lotes)
        boolean tieneRelacion = loteProductoRepository.existsByProducto(producto);
        if (tieneRelacion) {
            throw new IllegalStateException("No se puede eliminar el producto porque tiene lotes registrados.");
        }

        productoRepository.delete(producto);
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

    public Workbook generarReporteStockActualExcel() {
        List<Producto> productos = productoRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Stock Actual");

        Row header = sheet.createRow(0);
        String[] columnas = {
                "ID", "Código SKU", "Nombre", "Stock Actual",
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
            row.createCell(3).setCellValue(producto.getStockActual() != null ? producto.getStockActual().doubleValue() : 0);
            row.createCell(4).setCellValue(producto.getUnidadMedida() != null ? producto.getUnidadMedida().getNombre() : "");
            row.createCell(5).setCellValue(producto.getStockMinimo() != null ? producto.getStockMinimo().doubleValue() : 0);
            row.createCell(6).setCellValue(producto.isActivo());
            row.createCell(7).setCellValue(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getNombre() : "");
        }

        for (int i = 0; i < columnas.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    private TipoAnalisisCalidad obtenerTipoAnalisisDesdeDto(String valor) {
        if (valor == null || valor.isBlank()) {
            return TipoAnalisisCalidad.NINGUNO;
        }
        return TipoAnalisisCalidad.valueOf(valor);
    }
}

