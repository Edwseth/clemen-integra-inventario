package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.DetalleEtapa;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.DetalleEtapaRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetalleEtapaServiceImpl implements DetalleEtapaService {

    private final DetalleEtapaRepository repository;
    private final EtapaProduccionRepository etapaProduccionRepository;
    private final OrdenProduccionRepository ordenProduccionRepository;
    private final UsuarioRepository usuarioRepository;

    public List<DetalleEtapa> listarTodas() {
        return repository.findAll();
    }

    public Optional<DetalleEtapa> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public DetalleEtapa guardar(DetalleEtapa detalle) {
        try {
            if (detalle == null) {
                throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "DETALLE_OBLIGATORIO");
            }
            Long etapaId = detalle.getEtapaProduccion() != null ? detalle.getEtapaProduccion().getId() : null;
            Long ordenId = detalle.getOrdenProduccion() != null ? detalle.getOrdenProduccion().getId() : null;
            Long operarioId = detalle.getOperario() != null ? detalle.getOperario().getId() : null;

            if (etapaId == null || ordenId == null || operarioId == null) {
                throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                        "FALTAN_REFERENCIAS_REQUERIDAS");
            }

            EtapaProduccion etapa = etapaProduccionRepository.findById(etapaId)
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                            "ETAPA_NO_ENCONTRADA"));
            OrdenProduccion orden = ordenProduccionRepository.findById(ordenId)
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                            "ORDEN_NO_ENCONTRADA"));
            Usuario operario = usuarioRepository.findById(operarioId)
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                            "OPERARIO_NO_ENCONTRADO"));

            if (etapa.getOrdenProduccion() == null
                    || !etapa.getOrdenProduccion().getId().equals(orden.getId())) {
                throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "ETAPA_NO_PERTENECE_A_ORDEN");
            }

            detalle.setEtapaProduccion(etapa);
            detalle.setOrdenProduccion(orden);
            detalle.setOperario(operario);

            return repository.save(detalle);
        } catch (DataIntegrityViolationException ex) {
            log.error("Error de integridad al guardar detalle de etapa opId={} etapaId={} operarioId={}",
                    detalle != null && detalle.getOrdenProduccion() != null ? detalle.getOrdenProduccion().getId() : null,
                    detalle != null && detalle.getEtapaProduccion() != null ? detalle.getEtapaProduccion().getId() : null,
                    detalle != null && detalle.getOperario() != null ? detalle.getOperario().getId() : null,
                    ex);
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "DETALLE_ETAPA_DATOS_INVALIDOS");
        } catch (RuntimeException ex) {
            log.error("Error inesperado al guardar detalle de etapa", ex);
            throw ex;
        }
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
