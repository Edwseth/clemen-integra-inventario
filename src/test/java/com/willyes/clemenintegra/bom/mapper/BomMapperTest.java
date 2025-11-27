package com.willyes.clemenintegra.bom.mapper;

import com.willyes.clemenintegra.bom.dto.DetalleFormulaRequest;
import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class BomMapperTest {

    private final BomMapper mapper = Mappers.getMapper(BomMapper.class);

    @Test
    @DisplayName("toEntity asigna insumo y unidad de medida al detalle de fórmula")
    void toEntityAsignaRelaciones() {
        DetalleFormulaRequest request = new DetalleFormulaRequest();
        request.formulaId = 1L;
        request.insumoId = 2L;
        request.unidadMedidaId = 3L;
        request.cantidadNecesaria = 5.0;
        request.obligatorio = Boolean.TRUE;

        FormulaProducto formula = new FormulaProducto();
        formula.setId(1L);

        Producto insumo = new Producto();
        insumo.setId(2);

        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(3L);

        DetalleFormula detalle = mapper.toEntity(request, formula, insumo, unidad);

        assertThat(detalle.getInsumo()).isNotNull();
        assertThat(detalle.getInsumo().getId()).isEqualTo(2);
        assertThat(detalle.getUnidadMedida()).isNotNull();
        assertThat(detalle.getUnidadMedida().getId()).isEqualTo(3);
    }
}
