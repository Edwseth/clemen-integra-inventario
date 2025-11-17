package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.VidaUtilProductoDTO;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface VidaUtilProductoService {

    Optional<VidaUtilProducto> buscarPorProductoId(Integer productoId);

    VidaUtilProducto guardar(Integer productoId, Integer semanasVigencia);

    void eliminar(Integer productoId);

    Page<VidaUtilProductoDTO> listarProductosTerminados(String filtro, Pageable pageable);
}
