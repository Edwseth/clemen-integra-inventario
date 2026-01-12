package com.willyes.clemenintegra.planeacion.service.impl;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.planeacion.dto.PlanProduccionSemanalDTO;
import com.willyes.clemenintegra.planeacion.dto.PlanProduccionResumenDTO;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionDetalle;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.repository.PlanProduccionSemanalRepository;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanProduccionServiceImpl implements PlanProduccionService {

    private final PlanProduccionSemanalRepository planProduccionSemanalRepository;

    @Override
    public PlanProduccionSemanal crearOActualizar(PlanProduccionSemanalDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("El plan semanal no puede ser nulo");
        }

        PlanProduccionSemanal plan = Optional.ofNullable(dto.getId())
                .flatMap(planProduccionSemanalRepository::findById)
                .orElseGet(PlanProduccionSemanal::new);

        if (plan.getEstado() != null && plan.getEstado() != EstadoPlanProduccion.BORRADOR) {
            throw new IllegalStateException("Solo se pueden editar planes en estado BORRADOR");
        }

        plan.setSemanaInicio(dto.getSemanaInicio());
        plan.setSemanaFin(dto.getSemanaFin());
        plan.setComentarios(dto.getComentarios());
        if (plan.getEstado() == null) {
            plan.setEstado(EstadoPlanProduccion.BORRADOR);
        }
        if (dto.getCreadoPorId() != null && plan.getCreadoPor() == null) {
            plan.setCreadoPor(new Usuario(dto.getCreadoPorId()));
        }

        Usuario creador = plan.getCreadoPor();
        if (creador == null && dto.getCreadoPorId() != null) {
            creador = new Usuario(dto.getCreadoPorId());
            plan.setCreadoPor(creador);
        }
        final Usuario creadorFinal = creador;

        plan.getDetalles().clear();
        if (dto.getDetalles() != null) {
            dto.getDetalles().forEach(detalleDTO -> {
                PlanProduccionDetalle detalle = PlanProduccionDetalle.builder()
                        .plan(plan)
                        .producto(detalleDTO.getProductoId() != null ? new Producto(detalleDTO.getProductoId().intValue()) : null)
                        .cantidadPlanificada(detalleDTO.getCantidadPlanificada())
                        .unidadMedida(detalleDTO.getUnidadMedidaId() != null ? new UnidadMedida(detalleDTO.getUnidadMedidaId()) : null)
                        .prioridad(detalleDTO.getPrioridad())
                        .origenDemanda(detalleDTO.getOrigenDemanda())
                        .observacion(detalleDTO.getObservacion())
                        .creadoPor(creadorFinal)
                        .fechaCreacion(LocalDateTime.now())
                        .build();
                plan.getDetalles().add(detalle);
            });
        }

        if (plan.getSemanaInicio() == null || plan.getSemanaFin() == null) {
            throw new IllegalArgumentException("La semana objetivo debe tener fecha de inicio y fecha de fin.");
        }

        return planProduccionSemanalRepository.save(plan);
    }

    @Override
    public PlanProduccionSemanal confirmar(Long id) {
        PlanProduccionSemanal plan = planProduccionSemanalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan semanal no encontrado"));
        if (plan.getEstado() != EstadoPlanProduccion.BORRADOR) {
            throw new IllegalStateException("Solo los planes en BORRADOR pueden confirmarse");
        }
        plan.setEstado(EstadoPlanProduccion.CONFIRMADO);
        return planProduccionSemanalRepository.save(plan);
    }

    @Override
    public PlanProduccionSemanal cerrar(Long id) {
        PlanProduccionSemanal plan = planProduccionSemanalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan semanal no encontrado"));
        if (plan.getEstado() != EstadoPlanProduccion.CONFIRMADO) {
            throw new IllegalStateException("Solo los planes en CONFIRMADO pueden cerrarse");
        }
        plan.setEstado(EstadoPlanProduccion.CERRADO);
        // TODO: actualizar migración de base de datos si la columna estado usa un check/enum.
        return planProduccionSemanalRepository.save(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanProduccionSemanal> buscarPorId(Long id) {
        return planProduccionSemanalRepository.findWithDetallesById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlanProduccionResumenDTO> listar(LocalDate semanaInicioDesde, LocalDate semanaInicioHasta, EstadoPlanProduccion estado, Pageable pageable) {
        Pageable pageableToUse = pageable;
        if (pageable.getSort().isUnsorted()) {
            pageableToUse = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "semanaInicio"));
        }

        return planProduccionSemanalRepository.buscarPorFiltros(semanaInicioDesde, semanaInicioHasta, estado, pageableToUse)
                .map(this::toResumenDto);
    }

    private PlanProduccionResumenDTO toResumenDto(PlanProduccionSemanal plan) {
        String creadoPorNombre = null;
        if (plan.getCreadoPor() != null) {
            String nombreCompleto = plan.getCreadoPor().getNombreCompleto();
            if (nombreCompleto != null && !nombreCompleto.isBlank()) {
                creadoPorNombre = nombreCompleto;
            } else {
                creadoPorNombre = plan.getCreadoPor().getNombreUsuario();
            }
        }

        return PlanProduccionResumenDTO.builder()
                .id(plan.getId())
                .semanaInicio(plan.getSemanaInicio())
                .semanaFin(plan.getSemanaFin())
                .estado(plan.getEstado())
                .creadoPorNombre(creadoPorNombre)
                .fechaCreacion(plan.getFechaCreacion())
                .fechaConfirmacion(null)
                .build();
    }
}
