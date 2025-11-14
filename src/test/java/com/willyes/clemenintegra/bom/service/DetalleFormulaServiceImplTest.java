package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.DetalleFormulaRepository;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetalleFormulaServiceImplTest {

    @Mock
    private DetalleFormulaRepository detalleRepository;

    @Mock
    private FormulaProductoRepository formulaRepository;

    @InjectMocks
    private DetalleFormulaServiceImpl service;

    @Test
    @DisplayName("guardar permite crear detalle cuando la fórmula está en BORRADOR")
    void guardarPermiteBorrador() {
        FormulaProducto formula = new FormulaProducto();
        formula.setId(1L);
        formula.setEstado(EstadoFormula.BORRADOR);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setFormula(formula);

        when(formulaRepository.findById(1L)).thenReturn(Optional.of(formula));
        when(detalleRepository.save(any(DetalleFormula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> service.guardar(detalle)).doesNotThrowAnyException();
        verify(detalleRepository).save(detalle);
    }

    @Test
    @DisplayName("guardar permite actualizar detalle cuando la fórmula está en EN_REVISION")
    void guardarPermiteEnRevision() {
        FormulaProducto formula = new FormulaProducto();
        formula.setId(5L);
        formula.setEstado(EstadoFormula.EN_REVISION);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setFormula(formula);

        when(formulaRepository.findById(5L)).thenReturn(Optional.of(formula));
        when(detalleRepository.save(any(DetalleFormula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DetalleFormula resultado = service.guardar(detalle);

        assertThat(resultado.getFormula()).isEqualTo(formula);
        verify(detalleRepository).save(detalle);
    }

    @Test
    @DisplayName("guardar rechaza cambios cuando la fórmula está en APROBADA")
    void guardarRechazaCuandoFormulaAprobada() {
        FormulaProducto formula = new FormulaProducto();
        formula.setId(8L);
        formula.setEstado(EstadoFormula.APROBADA);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setFormula(formula);

        when(formulaRepository.findById(8L)).thenReturn(Optional.of(formula));

        assertThatThrownBy(() -> service.guardar(detalle))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessageContaining("APROBADA");

        verify(detalleRepository, never()).save(any(DetalleFormula.class));
    }

    @Test
    @DisplayName("eliminar rechaza cuando la fórmula está en APROBADA")
    void eliminarRechazaCuandoFormulaAprobada() {
        FormulaProducto formula = new FormulaProducto();
        formula.setId(10L);
        formula.setEstado(EstadoFormula.APROBADA);

        DetalleFormula existente = new DetalleFormula();
        existente.setId(22L);
        existente.setFormula(formula);

        when(detalleRepository.findById(22L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.eliminar(22L))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessageContaining("APROBADA");

        verify(detalleRepository, never()).delete(any(DetalleFormula.class));
    }
}
