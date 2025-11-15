package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.DetalleFormulaProduccionDTO;
import com.willyes.clemenintegra.bom.dto.FormulaActivaProduccionDTO;
import com.willyes.clemenintegra.bom.dto.FormulaProductoResumenDTO;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.DocumentoFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.model.enums.TipoDocumento;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.produccion.service.DisponibilidadInsumoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormulaProductoServiceImplTest {

    @Mock
    private FormulaProductoRepository formulaRepository;

    @Mock
    private LoteProductoRepository loteProductoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private DisponibilidadInsumoService disponibilidadInsumoService;

    private BomMapper bomMapper;

    private FormulaProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        bomMapper = Mappers.getMapper(BomMapper.class);
        service = new FormulaProductoServiceImpl(formulaRepository, bomMapper, loteProductoRepository, disponibilidadInsumoService, usuarioRepository);
    }

    @Test
    @DisplayName("cambiarEstado permite transición de BORRADOR a EN_REVISION")
    void cambiarEstadoDeBorradorARevision() {
        FormulaProducto formula = new FormulaProducto();
        formula.setId(1L);
        formula.setEstado(EstadoFormula.BORRADOR);
        formula.setActivo(false);
        formula.setProducto(new Producto());

        Usuario usuario = new Usuario();
        usuario.setId(5L);

        when(formulaRepository.findById(1L)).thenReturn(Optional.of(formula));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(formulaRepository.save(formula)).thenReturn(formula);

        FormulaProducto resultado = service.cambiarEstado(1L, EstadoFormula.EN_REVISION, 5L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoFormula.EN_REVISION);
        assertThat(resultado.isActivo()).isFalse();
        assertThat(resultado.getActualizadoPor()).isEqualTo(usuario);
        assertThat(resultado.getFechaActualizacion()).isNotNull();
        verify(formulaRepository, never()).desactivarOtrasFormulasDelProducto(any(), any());
        verify(formulaRepository).save(formula);
    }

    @Test
    @DisplayName("cambiarEstado aprueba fórmula y desactiva otras del producto")
    void cambiarEstadoApruebaFormula() {
        Producto producto = new Producto();
        producto.setId(2);

        FormulaProducto formula = new FormulaProducto();
        formula.setId(3L);
        formula.setEstado(EstadoFormula.EN_REVISION);
        formula.setActivo(false);
        formula.setProducto(producto);

        Usuario usuario = new Usuario();
        usuario.setId(7L);

        when(formulaRepository.findById(3L)).thenReturn(Optional.of(formula));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(formulaRepository.save(formula)).thenReturn(formula);

        FormulaProducto resultado = service.cambiarEstado(3L, EstadoFormula.APROBADA, 7L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoFormula.APROBADA);
        assertThat(resultado.isActivo()).isTrue();
        verify(formulaRepository).desactivarOtrasFormulasDelProducto(producto, 3L);
        verify(formulaRepository).save(formula);
    }

    @Test
    @DisplayName("cambiarEstado permite rechazar fórmula en revisión")
    void cambiarEstadoRechazaFormula() {
        FormulaProducto formula = new FormulaProducto();
        formula.setId(4L);
        formula.setEstado(EstadoFormula.EN_REVISION);
        formula.setActivo(true);
        formula.setProducto(new Producto());

        Usuario usuario = new Usuario();
        usuario.setId(8L);

        when(formulaRepository.findById(4L)).thenReturn(Optional.of(formula));
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(usuario));
        when(formulaRepository.save(formula)).thenReturn(formula);

        FormulaProducto resultado = service.cambiarEstado(4L, EstadoFormula.RECHAZADA, 8L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoFormula.RECHAZADA);
        assertThat(resultado.isActivo()).isFalse();
        verify(formulaRepository, never()).desactivarOtrasFormulasDelProducto(any(), any());
    }

    @Test
    @DisplayName("cambiarEstado rechaza transiciones inválidas")
    void cambiarEstadoTransicionInvalida() {
        FormulaProducto formula = new FormulaProducto();
        formula.setId(10L);
        formula.setEstado(EstadoFormula.BORRADOR);
        formula.setProducto(new Producto());

        when(formulaRepository.findById(10L)).thenReturn(Optional.of(formula));

        assertThatThrownBy(() -> service.cambiarEstado(10L, EstadoFormula.APROBADA, 1L))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.OPERACION_NO_PERMITIDA);
        verify(formulaRepository, never()).save(any());
    }

    @Test
    @DisplayName("cambiarEstado falla cuando la fórmula no existe")
    void cambiarEstadoFormulaNoExiste() {
        when(formulaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(999L, EstadoFormula.EN_REVISION, 1L))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.RECURSO_NO_ENCONTRADO);
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

        when(formulaRepository.findAllForResumen(null, null)).thenReturn(List.of(formula));

        List<FormulaProductoResumenDTO> resultado = service.listarResumen(null, null);

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

    @Test
    @DisplayName("listarResumen filtra por estado cuando se proporciona")
    void listarResumenFiltraPorEstado() {
        Producto producto = new Producto();
        producto.setId(2);
        producto.setCodigoSku("PR-002");
        producto.setNombre("Producto Borrador");

        Usuario responsable = new Usuario();
        responsable.setNombreCompleto("Responsable Borrador");

        FormulaProducto formula = new FormulaProducto();
        formula.setId(20L);
        formula.setProducto(producto);
        formula.setVersion("v3");
        formula.setEstado(EstadoFormula.BORRADOR);
        formula.setActivo(false);
        formula.setFechaActualizacion(LocalDateTime.of(2024, 2, 20, 9, 15));
        formula.setActualizadoPor(responsable);

        when(formulaRepository.findAllForResumen(EstadoFormula.BORRADOR, null)).thenReturn(List.of(formula));

        List<FormulaProductoResumenDTO> resultado = service.listarResumen(EstadoFormula.BORRADOR, null);

        assertThat(resultado).hasSize(1);
        FormulaProductoResumenDTO dto = resultado.get(0);
        assertThat(dto.estado).isEqualTo(EstadoFormula.BORRADOR.name());
        assertThat(dto.codigoProducto).isEqualTo("PR-002");
        verify(formulaRepository).findAllForResumen(EstadoFormula.BORRADOR, null);
    }

    @Test
    @DisplayName("listarResumen normaliza el filtro de producto antes de consultar el repositorio")
    void listarResumenFiltraPorTextoProducto() {
        Producto producto = new Producto();
        producto.setId(3);
        producto.setCodigoSku("PT-0311");
        producto.setNombre("CVC-COMPRIMIDO VITAMINA C 500 MG");

        Usuario responsable = new Usuario();
        responsable.setNombreCompleto("Responsable Texto");

        FormulaProducto formula = new FormulaProducto();
        formula.setId(30L);
        formula.setProducto(producto);
        formula.setVersion("v5");
        formula.setEstado(EstadoFormula.APROBADA);
        formula.setActivo(true);
        formula.setFechaActualizacion(LocalDateTime.of(2024, 3, 10, 8, 45));
        formula.setActualizadoPor(responsable);

        when(formulaRepository.findAllForResumen(any(), any())).thenReturn(List.of(formula));

        List<FormulaProductoResumenDTO> resultado = service.listarResumen(null, "  vitamina c   ");

        assertThat(resultado).hasSize(1);
        FormulaProductoResumenDTO dto = resultado.get(0);
        assertThat(dto.codigoProducto).isEqualTo("PT-0311");
        assertThat(dto.nombreProducto).contains("VITAMINA C");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(formulaRepository).findAllForResumen(isNull(), captor.capture());
        assertThat(captor.getValue()).isEqualTo("vitamina c");
    }

    @Test
    @DisplayName("obtenerFormulaActivaProduccion devuelve la fórmula aprobada activa con detalles mapeados")
    void obtenerFormulaActivaProduccionOk() {
        Producto producto = new Producto();
        producto.setId(1);
        producto.setCodigoSku("PR-001");
        producto.setNombre("Producto Terminado");

        Producto insumo = new Producto();
        insumo.setId(2);
        insumo.setCodigoSku("INS-001");
        insumo.setNombre("Insumo Principal");

        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(10L);
        unidad.setNombre("Kilogramo");
        unidad.setSimbolo("KG");

        DetalleFormula detalle = new DetalleFormula();
        detalle.setId(5L);
        detalle.setInsumo(insumo);
        detalle.setUnidadMedida(unidad);
        detalle.setCantidadNecesaria(new BigDecimal("2.5000"));
        detalle.setObligatorio(true);

        Usuario responsable = new Usuario();
        responsable.setNombreCompleto("Responsable Producción");

        FormulaProducto formula = new FormulaProducto();
        formula.setId(3L);
        formula.setProducto(producto);
        formula.setVersion("v2");
        formula.setEstado(EstadoFormula.APROBADA);
        formula.setActivo(true);
        formula.setFechaActualizacion(LocalDateTime.of(2024, 5, 1, 12, 0));
        formula.setActualizadoPor(responsable);
        formula.setDetalles(List.of(detalle));

        when(formulaRepository.findByProductoIdAndEstadoAndActivoTrue(1L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        FormulaActivaProduccionDTO resultado = service.obtenerFormulaActivaProduccion(1L);

        assertThat(resultado.formulaId).isEqualTo(3L);
        assertThat(resultado.productoId).isEqualTo(1L);
        assertThat(resultado.codigoProducto).isEqualTo("PR-001");
        assertThat(resultado.nombreProducto).isEqualTo("Producto Terminado");
        assertThat(resultado.version).isEqualTo("v2");
        assertThat(resultado.estado).isEqualTo(EstadoFormula.APROBADA);
        assertThat(resultado.activo).isTrue();
        assertThat(resultado.fechaActualizacion).isEqualTo(LocalDateTime.of(2024, 5, 1, 12, 0));
        assertThat(resultado.usuarioResponsable).isEqualTo("Responsable Producción");
        assertThat(resultado.detalles).hasSize(1);

        DetalleFormulaProduccionDTO detalleDTO = resultado.detalles.get(0);
        assertThat(detalleDTO.detalleId).isEqualTo(5L);
        assertThat(detalleDTO.productoInsumoId).isEqualTo(2L);
        assertThat(detalleDTO.codigoInsumo).isEqualTo("INS-001");
        assertThat(detalleDTO.nombreInsumo).isEqualTo("Insumo Principal");
        assertThat(detalleDTO.unidadMedidaId).isEqualTo(10L);
        assertThat(detalleDTO.nombreUnidadMedida).isEqualTo("Kilogramo");
        assertThat(detalleDTO.simboloUnidadMedida).isEqualTo("KG");
        assertThat(detalleDTO.cantidadNecesaria).isEqualByComparingTo(new BigDecimal("2.5000"));
        assertThat(detalleDTO.obligatorio).isTrue();
    }

    @Test
    @DisplayName("obtenerFormulaActivaProduccion lanza excepción cuando no existe fórmula activa aprobada")
    void obtenerFormulaActivaProduccionSinFormulaActiva() {
        when(formulaRepository.findByProductoIdAndEstadoAndActivoTrue(1L, EstadoFormula.APROBADA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerFormulaActivaProduccion(1L))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.OPERACION_NO_PERMITIDA)
                .hasMessage("El producto seleccionado no tiene una fórmula activa aprobada.");
    }

    @Test
    @DisplayName("clonarFormula genera nueva versión en borrador con detalles y documentos")
    void clonarFormulaGeneraNuevaVersion() {
        Producto producto = new Producto();
        producto.setId(5);
        producto.setCodigoSku("PR-005");

        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(2L);
        unidad.setNombre("Kilogramo");

        Producto insumo = new Producto();
        insumo.setId(9);
        insumo.setNombre("Insumo A");

        DetalleFormula detalleOriginal = new DetalleFormula();
        detalleOriginal.setInsumo(insumo);
        detalleOriginal.setUnidadMedida(unidad);
        detalleOriginal.setCantidadNecesaria(BigDecimal.valueOf(2));
        detalleOriginal.setObligatorio(Boolean.TRUE);

        DocumentoFormula documentoOriginal = DocumentoFormula.builder()
                .tipoDocumento(TipoDocumento.PROCEDIMIENTO)
                .nombreArchivo("doc.pdf")
                .rutaArchivo("/files/doc.pdf")
                .build();

        FormulaProducto origen = new FormulaProducto();
        origen.setId(7L);
        origen.setProducto(producto);
        origen.setVersion("v3");
        origen.setObservacion("Observación previa");
        origen.setDetalles(List.of(detalleOriginal));
        origen.setDocumentos(List.of(documentoOriginal));
        detalleOriginal.setFormula(origen);
        documentoOriginal.setFormula(origen);

        FormulaProducto versionAnterior = new FormulaProducto();
        versionAnterior.setId(4L);
        versionAnterior.setProducto(producto);
        versionAnterior.setVersion("v2");

        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setNombreCompleto("Usuario Clonador");

        when(formulaRepository.findById(7L)).thenReturn(Optional.of(origen));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));
        when(formulaRepository.findAllByProductoId(5L)).thenReturn(List.of(versionAnterior, origen));
        when(formulaRepository.save(any(FormulaProducto.class))).thenAnswer(invocation -> {
            FormulaProducto guardado = invocation.getArgument(0);
            guardado.setId(11L);
            return guardado;
        });

        FormulaProducto clon = service.clonarFormula(7L, 3L);

        assertThat(clon.getId()).isEqualTo(11L);
        assertThat(clon.getProducto()).isEqualTo(producto);
        assertThat(clon.getVersion()).isEqualTo("v4");
        assertThat(clon.getEstado()).isEqualTo(EstadoFormula.BORRADOR);
        assertThat(clon.isActivo()).isFalse();
        assertThat(clon.getCreadoPor()).isEqualTo(usuario);
        assertThat(clon.getActualizadoPor()).isEqualTo(usuario);
        assertThat(clon.getObservacion()).isEqualTo("Observación previa");
        assertThat(clon.getFechaCreacion()).isNotNull();
        assertThat(clon.getFechaActualizacion()).isNotNull();

        assertThat(clon.getDetalles()).hasSize(1);
        DetalleFormula detalleClonado = clon.getDetalles().get(0);
        assertThat(detalleClonado.getId()).isNull();
        assertThat(detalleClonado.getFormula()).isEqualTo(clon);
        assertThat(detalleClonado.getInsumo()).isEqualTo(insumo);
        assertThat(detalleClonado.getUnidadMedida()).isEqualTo(unidad);
        assertThat(detalleClonado.getCantidadNecesaria()).isEqualTo(BigDecimal.valueOf(2));
        assertThat(detalleClonado.getObligatorio()).isTrue();

        assertThat(clon.getDocumentos()).hasSize(1);
        DocumentoFormula documentoClonado = clon.getDocumentos().get(0);
        assertThat(documentoClonado.getId()).isNull();
        assertThat(documentoClonado.getFormula()).isEqualTo(clon);
        assertThat(documentoClonado.getTipoDocumento()).isEqualTo(TipoDocumento.PROCEDIMIENTO);
        assertThat(documentoClonado.getNombreArchivo()).isEqualTo("doc.pdf");
        assertThat(documentoClonado.getRutaArchivo()).isEqualTo("/files/doc.pdf");
    }

    @Test
    @DisplayName("clonarFormula lanza excepción si la fórmula no existe")
    void clonarFormulaFormulaNoExiste() {
        when(formulaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clonarFormula(999L, 1L))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.RECURSO_NO_ENCONTRADO);
    }
}

