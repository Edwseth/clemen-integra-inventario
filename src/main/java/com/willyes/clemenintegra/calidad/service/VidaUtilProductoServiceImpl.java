package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.VidaUtilProductoDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.VidaUtilProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(readOnly = true)
    public Optional<VidaUtilProducto> buscarPorProductoId(Integer productoId) {
        return vidaUtilProductoRepository.findById(productoId);
    }

    @Override
    @Transactional
    public VidaUtilProducto guardar(Integer productoId, Integer semanasVigencia) {
        Producto producto = productoRepository.findById(Long.valueOf(productoId))
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "Producto no encontrado"));

        if (!esProductoTerminado(producto)) {
            throw new CustomBusinessException(ApiErrorCode.VIDA_UTIL_SOLO_PRODUCTO_TERMINADO,
                    "La vida útil solo puede configurarse para productos terminados");
        }
        if (semanasVigencia == null || semanasVigencia <= 0) {
            throw new CustomBusinessException(ApiErrorCode.VIDA_UTIL_SEMANAS_INVALIDAS,
                    "Las semanas de vigencia deben ser mayores a cero");
        }

        VidaUtilProducto vidaUtil = vidaUtilProductoRepository.findById(productoId)
                .orElseGet(VidaUtilProducto::new);
        vidaUtil.setProducto(producto);
        vidaUtil.setProductoId(productoId);
        vidaUtil.setSemanasVigencia(semanasVigencia);

        return vidaUtilProductoRepository.save(vidaUtil);
    }

    @Override
    @Transactional
    public void eliminar(Integer productoId) {
        Producto producto = productoRepository.findById(Long.valueOf(productoId))
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "Producto no encontrado"));
        if (!esProductoTerminado(producto)) {
            throw new CustomBusinessException(ApiErrorCode.VIDA_UTIL_SOLO_PRODUCTO_TERMINADO,
                    "La vida útil solo puede configurarse para productos terminados");
        }
        vidaUtilProductoRepository.findById(productoId)
                .ifPresent(vidaUtilProductoRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VidaUtilProductoDTO> listarProductosTerminados(String filtro, Pageable pageable) {
        Specification<Producto> spec = Specification
                .where(textoLibre(filtro))
                .and(tipoCategoriaEquals(TipoCategoria.PRODUCTO_TERMINADO));

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
                .build());
    }

    private boolean esProductoTerminado(Producto producto) {
        return producto != null
                && producto.getCategoriaProducto() != null
                && producto.getCategoriaProducto().getTipo() == TipoCategoria.PRODUCTO_TERMINADO;
    }
}
