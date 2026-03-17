package com.willyes.clemenintegra.inventario.config;

import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.DateTimeException;
import java.time.ZoneId;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "inventory.vencidos")
public class InventoryVencidosProperties {

    private final Job job = new Job();
    private EstadoLote estadoObjetivo = EstadoLote.VENCIDO;
    @Min(1)
    private int chunkSize = 500;
    private final Movimiento movimiento = new Movimiento();
    private Long almacenDestinoId;
    private String almacenDestinoNombre = "Obsoletos";

    public ZoneId resolveZoneId() {
        try {
            return ZoneId.of(job.getTimezone());
        } catch (DateTimeException ex) {
            throw new IllegalStateException("Zona horaria inválida para inventory.vencidos.job.timezone", ex);
        }
    }

    public void validateForExecution() {
        if (chunkSize < 1) {
            throw new IllegalStateException("inventory.vencidos.chunkSize debe ser mayor a cero");
        }
        movimiento.validate(almacenDestinoId);
    }

    public Long requireAlmacenDestinoId() {
        if (almacenDestinoId == null) {
            throw new IllegalStateException("inventory.vencidos.almacenDestinoId es obligatorio cuando movimiento.enabled=true");
        }
        return almacenDestinoId;
    }

    public String requireAlmacenDestinoNombre() {
        if (almacenDestinoNombre == null || almacenDestinoNombre.isBlank()) {
            throw new IllegalStateException("inventory.vencidos.almacenDestinoNombre es obligatorio cuando no se configura almacenDestinoId");
        }
        return almacenDestinoNombre.trim();
    }

    @AssertTrue(message = "inventory.vencidos.estadoObjetivo solo admite VENCIDO o RECHAZADO")
    public boolean isEstadoObjetivoValido() {
        return estadoObjetivo == null
                || estadoObjetivo == EstadoLote.VENCIDO
                || estadoObjetivo == EstadoLote.RECHAZADO;
    }

    @Data
    public static class Job {
        private boolean enabled = true;
        private String cron = "0 0 0 * * *";
        private String timezone = "America/Bogota";
    }

    @Data
    public static class Movimiento {
        private boolean enabled = true;
        private Long motivoId;
        private String clasificacion;

        public ClasificacionMovimientoInventario resolveClasificacionEnum() {
            if (clasificacion == null || clasificacion.isBlank()) {
                throw new IllegalStateException("inventory.vencidos.movimiento.clasificacion es obligatorio cuando movimiento.enabled=true");
            }
            try {
                return ClasificacionMovimientoInventario.valueOf(clasificacion.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Clasificación inválida para inventory.vencidos.movimiento.clasificacion: " + clasificacion, ex);
            }
        }

        private void validate(Long almacenDestinoId) {
            if (!enabled) {
                return;
            }
            if (motivoId == null || motivoId <= 0) {
                throw new IllegalStateException("inventory.vencidos.movimiento.motivoId es obligatorio y debe ser positivo");
            }
            if (clasificacion == null || clasificacion.isBlank()) {
                throw new IllegalStateException("inventory.vencidos.movimiento.clasificacion es obligatorio cuando movimiento.enabled=true");
            }
        }
    }
}
