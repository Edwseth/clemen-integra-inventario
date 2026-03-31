package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LoteProductoMapperTest {

    private final LoteProductoMapper mapper = Mappers.getMapper(LoteProductoMapper.class);

    @Test
    void incluyeAlertaYUbicacionInternaEnRespuesta() {
        LoteProducto lote = new LoteProducto();
        lote.setCodigoLote("LOT-01");
        lote.setEstado(EstadoLote.EN_CUARENTENA);
        lote.setFechaVencimiento(LocalDateTime.now().minusDays(1));
        lote.setCodigoUbicacionInterna("A-01-02");
        lote.setDescripcionUbicacionInterna("Pasillo A, nivel 1");
        lote.setAlmacen(new Almacen(10));

        LoteProductoResponseDTO dto = mapper.toDto(lote);

        assertThat(dto.getAlerta()).isEqualTo("VENCIDO");
        assertThat(dto.getCodigoUbicacionInterna()).isEqualTo("A-01-02");
        assertThat(dto.getDescripcionUbicacionInterna()).isEqualTo("Pasillo A, nivel 1");
    }

    @Test
    void mapeaTotalIngresadoMaterialYCalculaValorStock() {
        LoteProducto lote = new LoteProducto();
        lote.setStockLote(new BigDecimal("12.500000"));
        lote.setTotalIngresadoMaterial(new BigDecimal("14.000000"));
        lote.setCostoUnitarioMaterial(new BigDecimal("3.200000"));

        LoteProductoResponseDTO dto = mapper.toResponseDTO(lote);

        assertThat(dto.getTotalIngresadoMaterial()).isEqualByComparingTo("14.000000");
        assertThat(dto.getValorStock()).isEqualByComparingTo("40.000000000000");
    }

    @Test
    void valorStockEsNullCuandoFaltaCostoUnitarioMaterial() {
        LoteProducto lote = new LoteProducto();
        lote.setStockLote(new BigDecimal("5.000000"));
        lote.setCostoUnitarioMaterial(null);

        LoteProductoResponseDTO dto = mapper.toResponseDTO(lote);

        assertThat(dto.getValorStock()).isNull();
    }
}
