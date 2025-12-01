package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.VidaUtilProductoDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.VidaUtilProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.willyes.clemenintegra.inventario.service.spec.ProductoSpecifications.*;

@Service
@RequiredArgsConstructor
public class VidaUtilProductoServiceImpl implements VidaUtilProductoService {

    private final VidaUtilProductoRepository vidaUtilProductoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioService usuarioService;

    @Override
    @Transactional(readOnly = true)
    public Optional<VidaUtilProducto> buscarPorProductoId(Integer productoId) {
        return vidaUtilProductoRepository.findById(productoId);
    }

    @Override
    @Transactional
    public VidaUtilProducto guardar(Integer productoId, Integer semanasVigencia) {
        if (productoId == null) {
            throw new CustomBusinessException(
                    ApiErrorCode.NEGOCIO_GENERICO,
                    "El identificador del producto es obligatorio para registrar vida útil"
            );
        }

        Producto producto = productoRepository.findById(Long.valueOf(productoId))
                .orElseThrow(() -> new CustomBusinessException(
                        ApiErrorCode.RECURSO_NO_ENCONTRADO,
                        "Producto no encontrado"
                ));

        validarProductoAdmiteVidaUtil(producto);

        Integer productoIdPersistencia = Optional.ofNullable(producto.getId())
                .orElseThrow(() -> new CustomBusinessException(
                        ApiErrorCode.NEGOCIO_GENERICO,
                        "El producto no posee identificador para registrar vida útil"
                ));

        if (semanasVigencia == null || semanasVigencia <= 0) {
            throw new CustomBusinessException(
                    ApiErrorCode.VIDA_UTIL_SEMANAS_INVALIDAS,
                    "Las semanas de vigencia deben ser mayores a cero"
            );
        }

        VidaUtilProducto vidaUtil = vidaUtilProductoRepository.findById(productoIdPersistencia)
                .orElseGet(() -> {
                    VidaUtilProducto nuevo = new VidaUtilProducto();
                    nuevo.setProductoId(productoIdPersistencia);
                    return nuevo;
                });

        vidaUtil.setProductoId(productoIdPersistencia);
        vidaUtil.setProducto(producto);
        vidaUtil.setSemanasVigencia(semanasVigencia);

        Usuario usuarioActual = usuarioService.obtenerUsuarioAutenticado();
        vidaUtil.setActualizadoPor(usuarioActual);
        vidaUtil.setFechaActualizacion(LocalDateTime.now());

        return vidaUtilProductoRepository.save(vidaUtil);
    }


    @Override
    @Transactional
    public void eliminar(Integer productoId) {
        Producto producto = productoRepository.findById(Long.valueOf(productoId))
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "Producto no encontrado"));
        validarProductoAdmiteVidaUtil(producto);
        vidaUtilProductoRepository.findById(productoId)
                .ifPresent(vidaUtilProductoRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VidaUtilProductoDTO> listarProductosTerminados(String filtro, Pageable pageable) {
        Specification<Producto> spec = Specification
                .where(textoLibre(filtro))
                .and(tipoCategoriaIn(List.of(
                        TipoCategoria.PRODUCTO_TERMINADO,
                        TipoCategoria.PRODUCTO_SEMI_ELABORADO
                )));

        Page<Producto> productos = productoRepository.findAll(spec, pageable);
        Map<Integer, VidaUtilProducto> vidaUtilMap = vidaUtilProductoRepository
                .findAllById(productos.stream().map(Producto::getId).toList())
                .stream()
                .collect(Collectors.toMap(VidaUtilProducto::getProductoId, Function.identity()));

        return productos.map(p -> VidaUtilProductoDTO.builder()
                .productoId(p.getId())
                .codigoSku(p.getCodigoSku())
                .nombreProducto(p.getNombre())
                .semanasVigencia(Optional.ofNullable(vidaUtilMap.get(p.getId()))
                        .map(VidaUtilProducto::getSemanasVigencia)
                        .orElse(null))
                .actualizadoPorNombre(Optional.ofNullable(vidaUtilMap.get(p.getId()))
                        .map(VidaUtilProducto::getActualizadoPor)
                        .map(Usuario::getNombreCompleto)
                        .orElse(null))
                .fechaActualizacion(Optional.ofNullable(vidaUtilMap.get(p.getId()))
                        .map(VidaUtilProducto::getFechaActualizacion)
                        .orElse(null))
                .build());
    }

    private void validarProductoAdmiteVidaUtil(Producto producto) {
        if (producto == null || producto.getCategoriaProducto() == null) {
            throw new IllegalArgumentException("El producto es obligatorio para definir vida útil.");
        }

        TipoCategoria tipo = producto.getCategoriaProducto().getTipo();
        if (tipo != TipoCategoria.PRODUCTO_TERMINADO
                && tipo != TipoCategoria.PRODUCTO_SEMI_ELABORADO) {
            throw new CustomBusinessException(
                    ApiErrorCode.VIDA_UTIL_SOLO_PRODUCTO_TERMINADO,
                    "Solo se puede registrar vida útil para productos terminados o semielaborados."
            );
        }
    }
}
