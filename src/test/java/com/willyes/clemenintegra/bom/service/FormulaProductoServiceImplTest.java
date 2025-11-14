package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.FormulaProductoResumenDTO;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormulaProductoServiceImplTest {

    @Mock
    private FormulaProductoRepository formulaRepository;

    @Mock
    private LoteProductoRepository loteProductoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private BomMapper bomMapper;

    private FormulaProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        bomMapper = Mappers.getMapper(BomMapper.class);
        service = new FormulaProductoServiceImpl(formulaRepository, bomMapper, loteProductoRepository, usuarioRepository);
    }

    @Test
    @DisplayName("listarResumen devuelve los campos necesarios sin detalles ni documentos")
    void listarResumenDevuelveCamposClaves() {
        Producto producto = new Producto();
        producto.setId(1);
        producto.setCodigoSku("PR-001");
        producto.setNombre("Producto Test");

        Usuario responsable = new Usuario();
        responsable.setNombreCompleto("Responsable Calidad");

        FormulaProducto formula = new FormulaProducto();
        formula.setId(10L);
        formula.setProducto(producto);
        formula.setVersion("v1");
        formula.setEstado(EstadoFormula.BORRADOR);
        formula.setActivo(true);
        formula.setFechaActualizacion(LocalDateTime.of(2024, 1, 15, 10, 30));
        formula.setActualizadoPor(responsable);

        when(formulaRepository.findAllForResumen()).thenReturn(List.of(formula));

        List<FormulaProductoResumenDTO> resultado = service.listarResumen();

        assertThat(resultado).hasSize(1);
        FormulaProductoResumenDTO dto = resultado.get(0);
        assertThat(dto.id).isEqualTo(10L);
        assertThat(dto.productoId).isEqualTo(1L);
        assertThat(dto.codigoProducto).isEqualTo("PR-001");
        assertThat(dto.nombreProducto).isEqualTo("Producto Test");
        assertThat(dto.version).isEqualTo("v1");
        assertThat(dto.estado).isEqualTo("BORRADOR");
        assertThat(dto.activo).isTrue();
        assertThat(dto.fechaActualizacion).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30));
        assertThat(dto.usuarioResponsable).isEqualTo("Responsable Calidad");

        assertThat(Arrays.stream(FormulaProductoResumenDTO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("detalles", "documentos");
    }
}

