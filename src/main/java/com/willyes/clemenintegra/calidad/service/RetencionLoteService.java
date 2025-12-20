package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RetencionLoteService {
    Page<RetencionLoteDTO> listar(EstadoRetencion estado, Pageable pageable);

    RetencionLoteDTO crear(RetencionLoteDTO dto);

    RetencionLoteDTO actualizar(Long id, RetencionLoteDTO dto);

    RetencionLoteDTO obtenerPorId(Long id);

    void eliminar(Long id);

    RetencionLote retener(Long loteId,
                          MotivoRetencion motivo,
                          String descripcion,
                          NoConformidad noConformidad,
                          Usuario usuario);

    RetencionLote retenerLote(Long loteId,
                              MotivoRetencion motivo,
                              String descripcion,
                              NoConformidad noConformidad,
                              Usuario usuario,
                              boolean moverACuarentena);

    RetencionLote asegurarRetencionNoConformidad(LoteProducto lote,
                                                  String descripcion,
                                                  NoConformidad noConformidad,
                                                  Usuario usuario);

    RetencionLote levantar(Long retencionId, Usuario usuario);

    RetencionLote levantarRetencion(Long retencionId, Usuario usuario);

    Optional<RetencionLote> obtenerActivaPorLote(Long loteId);

    List<RetencionLote> obtenerRetencionesActivas(Long loteId);
}
