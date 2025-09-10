package com.willyes.clemenintegra.inventario.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "inventory")
public class InventoryCatalogProperties {

    private final Almacen almacen = new Almacen();
    private final Motivo motivo = new Motivo();
    private final TipoDetalle tipoDetalle = new TipoDetalle();
    private final Produccion produccion = new Produccion();

    @Data
    public static class Almacen {
        private final IdHolder pt = new IdHolder();
        private final IdHolder cuarentena = new IdHolder();
        private final IdHolder obsoletos = new IdHolder();
    }

    @Data
    public static class Produccion {
        private final AlmacenProduccion almacen = new AlmacenProduccion();
    }

    @Data
    public static class AlmacenProduccion {
        private final Origen origen = new Origen();
    }

    @Data
    public static class Origen {
        private Long bodegaPrincipal;
        private Long preBodegaProduccion;
    }

    @Data
    public static class IdHolder {
        private Long id;
    }

    @Data
    public static class Motivo {
        private String entradaPt;
        private String transferenciaCalidad;
        private String devolucionDesdeProduccion;
        private String ajusteRechazo;
    }

    @Data
    public static class TipoDetalle {
        private Long entradaId;
        private Long transferenciaId;
        private Long salidaId;
    }
}
