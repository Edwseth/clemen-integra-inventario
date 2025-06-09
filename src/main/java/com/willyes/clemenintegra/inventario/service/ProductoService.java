package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.mapper.ProductoMapper;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ProductoMapper productoMapper;


    public List<ProductoResponseDTO> listarTodos() {
        return productoRepository.findAll()
                .stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
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

        var usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Producto producto = Producto.builder()
                .codigoSku(dto.getCodigoSku())
                .nombre(dto.getNombre())
                .descripcionProducto(dto.getDescripcionProducto())
                .stockActual(dto.getStockActual())
                .stockMinimo(dto.getStockMinimo())
                .stockMinimoProveedor(dto.getStockMinimoProveedor())
                .activo(dto.getActivo())
                .requiereInspeccion(dto.getRequiereInspeccion())
                .fechaCreacion(LocalDateTime.now())
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .build();

        productoRepository.save(producto);
        return mapearADTO(producto);
    }

    private void validarDuplicados(String sku, String nombre) {
        if (productoRepository.existsByCodigoSku(sku)) {
            throw new DataIntegrityViolationException("Ya existe un producto con ese código SKU.");
        }
        if (productoRepository.existsByNombre(nombre)) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre.");
        }
    }

    private ProductoResponseDTO mapearADTO(Producto producto) {
        return ProductoResponseDTO.builder()
                .id(producto.getId())
                .codigoSku(producto.getCodigoSku())
                .nombre(producto.getNombre())
                .descripcionProducto(producto.getDescripcionProducto())
                .stockActual(producto.getStockActual())
                .stockMinimo(producto.getStockMinimo())
                .activo(producto.isActivo())
                .requiereInspeccion(producto.isRequiereInspeccion())
                .unidadMedida(producto.getUnidadMedida().getNombre())
                .categoria(producto.getCategoriaProducto().getNombre())
                .fechaCreacion(producto.getFechaCreacion())
                .build();
    }

    public ProductoResponseDTO obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        return mapearADTO(producto);
    }

    public ProductoResponseDTO actualizarProducto(Long id, ProductoRequestDTO dto) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        // Validar duplicidad si se cambia el nombre o SKU
        if (!producto.getCodigoSku().equals(dto.getCodigoSku()) &&
                productoRepository.existsByCodigoSku(dto.getCodigoSku())) {
            throw new IllegalArgumentException("Ya existe otro producto con el mismo código SKU.");
        }

        if (!producto.getNombre().equals(dto.getNombre()) &&
                productoRepository.existsByNombre(dto.getNombre())) {
            throw new IllegalArgumentException("Ya existe otro producto con el mismo nombre.");
        }

        var unidad = unidadMedidaRepository.findById(dto.getUnidadMedidaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada"));

        var categoria = categoriaProductoRepository.findById(dto.getCategoriaProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        var usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Actualizar campos
        producto.setCodigoSku(dto.getCodigoSku());
        producto.setNombre(dto.getNombre());
        producto.setDescripcionProducto(dto.getDescripcionProducto());
        producto.setStockActual(dto.getStockActual());
        producto.setStockMinimo(dto.getStockMinimo());
        producto.setStockMinimoProveedor(dto.getStockMinimoProveedor());
        producto.setActivo(dto.getActivo());
        producto.setRequiereInspeccion(dto.getRequiereInspeccion());
        producto.setUnidadMedida(unidad);
        producto.setCategoriaProducto(categoria);
        producto.setCreadoPor(usuario);

        productoRepository.save(producto);

        return mapearADTO(producto);
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
                .map(lote -> {
                    Producto producto = lote.getProducto();
                    return new ProductoConEstadoLoteDTO(
                            producto.getId(),
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
                .collect(Collectors.groupingBy(LoteProducto::getProducto));

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
                            producto.getId(),
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

}

