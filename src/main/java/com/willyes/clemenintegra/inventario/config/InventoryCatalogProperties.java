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
    private final Mov mov = new Mov();
    private final Um um = new Um();

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
        private Long materiaPrima;
        private Long materialEmpaque;
        private Long suministros;
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
    }

    @Data
    public static class TipoDetalle {
        private Long entradaId;
        private Long transferenciaId;
        private Long salidaId;
        private Long salidaPtId;
    }

    @Data
    public static class Mov {
        private final MotivoMov motivo = new MotivoMov();

        @Data
        public static class MotivoMov {
            private Long ajusteRechazo;
        }
    }

    @Data
    public static class Um {
        private final Decimales decimales = new Decimales();
        private String redondeo;
    }

    @Data
    public static class Decimales {
        private int min;
        private int max;
    }
}
