package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.DetalleFormulaProduccionDTO;
import com.willyes.clemenintegra.bom.dto.DetalleFormulaResponse;
import com.willyes.clemenintegra.bom.dto.FormulaActivaProduccionDTO;
import com.willyes.clemenintegra.bom.dto.FormulaProductoResponse;
import com.willyes.clemenintegra.bom.dto.FormulaProductoSelectorDTO;
import com.willyes.clemenintegra.bom.dto.LoteResumenDTO;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.DocumentoFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.model.enums.TipoDocumento;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.produccion.service.DisponibilidadInsumoService;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
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
        verify(formulaRepository, never()).desactivarOtrasFormulasDelProducto(any(), any(), any(), any());
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
        verify(formulaRepository).desactivarOtrasFormulasDelProducto(eq(producto), eq(3L), eq(usuario), any());
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
        verify(formulaRepository, never()).desactivarOtrasFormulasDelProducto(any(), any(), any(), any());
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
        FormulaProductoSelectorDTO selector = new FormulaProductoSelectorDTO(
                10L,
                "v1",
                EstadoFormula.BORRADOR,
                1L,
                "PR-001",
                "Producto Test",
                true,
                LocalDateTime.of(2024, 1, 10, 8, 30),
                "Usuario Responsable",
                LocalDateTime.of(2024, 1, 9, 8, 30),
                "Usuario Creador");

        when(formulaRepository.findAllForSelector(isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(selector)));

        Page<FormulaProductoSelectorDTO> resultado = service.listarResumen(null, null, PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
        FormulaProductoSelectorDTO dto = resultado.getContent().get(0);
        assertThat(dto.formulaId).isEqualTo(10L);
        assertThat(dto.productoId).isEqualTo(1L);
        assertThat(dto.productoSku).isEqualTo("PR-001");
        assertThat(dto.productoNombre).isEqualTo("Producto Test");
        assertThat(dto.version).isEqualTo("v1");
        assertThat(dto.estado).isEqualTo("BORRADOR");
        assertThat(dto.activo).isTrue();
        assertThat(dto.fechaActualizacion).isNotNull();
        assertThat(dto.actualizadoPorNombre).isEqualTo("Usuario Responsable");
        assertThat(dto.fechaCreacion).isNotNull();
        assertThat(dto.creadoPorNombre).isEqualTo("Usuario Creador");
    }

    @Test
    @DisplayName("listarResumen filtra por estado cuando se proporciona")
    void listarResumenFiltraPorEstado() {
        FormulaProductoSelectorDTO selector = new FormulaProductoSelectorDTO(
                20L,
                "v3",
                EstadoFormula.BORRADOR,
                2L,
                "PR-002",
                "Producto Borrador",
                false,
                null,
                null,
                LocalDateTime.of(2024, 1, 12, 10, 0),
                "Usuario Creador");

        when(formulaRepository.findAllForSelector(eq(EstadoFormula.BORRADOR), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(selector)));

        Page<FormulaProductoSelectorDTO> resultado = service.listarResumen(EstadoFormula.BORRADOR, null, PageRequest.of(0, 5));

        assertThat(resultado.getContent()).hasSize(1);
        FormulaProductoSelectorDTO dto = resultado.getContent().get(0);
        assertThat(dto.estado).isEqualTo(EstadoFormula.BORRADOR.name());
        assertThat(dto.productoSku).isEqualTo("PR-002");
        verify(formulaRepository).findAllForSelector(eq(EstadoFormula.BORRADOR), isNull(), any(PageRequest.class));
    }

    @Test
    @DisplayName("listarResumen normaliza el filtro de producto antes de consultar el repositorio")
    void listarResumenFiltraPorTextoProducto() {
        FormulaProductoSelectorDTO selector = new FormulaProductoSelectorDTO(
                30L,
                "v5",
                EstadoFormula.APROBADA,
                3L,
                "PT-0311",
                "CVC-COMPRIMIDO VITAMINA C 500 MG",
                true,
                LocalDateTime.of(2024, 1, 15, 10, 0),
                "Usuario Responsable",
                LocalDateTime.of(2024, 1, 14, 10, 0),
                "Usuario Creador");

        when(formulaRepository.findAllForSelector(any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(selector)));

        Page<FormulaProductoSelectorDTO> resultado = service.listarResumen(null, "  vitamina c   ", PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
        FormulaProductoSelectorDTO dto = resultado.getContent().get(0);
        assertThat(dto.productoSku).isEqualTo("PT-0311");
        assertThat(dto.productoNombre).contains("VITAMINA C");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(formulaRepository).findAllForSelector(isNull(), captor.capture(), any(PageRequest.class));
        assertThat(captor.getValue()).isEqualTo("vitamina c");
    }

    @Test
    @DisplayName("obtenerFormulaActivaPorProducto refleja faltante FEFO cuando reservas reducen stock libre")
    void obtenerFormulaActivaPorProducto_reflejaFaltanteFefo() {
        Producto producto = new Producto();
        producto.setId(100);
        producto.setNombre("JVC-JARABE VITAMINA C 200ML");
        UnidadMedida umProducto = new UnidadMedida();
        umProducto.setSimbolo("UND");
        umProducto.setNombre("UNIDAD");
        umProducto.setNombrePlural("UNIDADES");
        producto.setUnidadMedida(umProducto);

        Producto insumo = new Producto();
        insumo.setId(200);
        insumo.setNombre("COLORANTE NATURAL ROJO");
        insumo.setCodigoSku("MP-COLRO");
        UnidadMedida umInsumo = new UnidadMedida();
        umInsumo.setSimbolo("MILILITRO");
        umInsumo.setNombre("MILILITRO");
        umInsumo.setNombrePlural("MILILITROS");
        insumo.setUnidadMedida(umInsumo);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(insumo);
        detalle.setCantidadNecesaria(new BigDecimal("0.5"));

        FormulaProducto formula = new FormulaProducto();
        formula.setId(50L);
        formula.setProducto(producto);
        formula.setDetalles(List.of(detalle));

        when(formulaRepository.findByProductoIdAndEstadoAndActivoTrue(100L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        when(loteProductoRepository.sumarPorEstado(200L)).thenReturn(List.of(
                new Object[]{EstadoLote.DISPONIBLE, new BigDecimal("1012.500000")},
                new Object[]{EstadoLote.RETENIDO, new BigDecimal("1500.000000")},
                new Object[]{EstadoLote.VENCIDO, new BigDecimal("1047.500000")}
        ));

        LocalDateTime futuro = LocalDateTime.now().plusMonths(6);
        when(loteProductoRepository.listarLotesPorProducto(200L)).thenReturn(List.of(
                new LoteResumenDTO(1L, "L-170925-3", EstadoLote.DISPONIBLE, "Principal MP", new BigDecimal("125.000000"), futuro, null, null),
                new LoteResumenDTO(2L, "L-271025-02", EstadoLote.DISPONIBLE, "Principal MP", new BigDecimal("130.000000"), futuro, null, null),
                new LoteResumenDTO(3L, "L-060825-3", EstadoLote.DISPONIBLE, "Principal MP", new BigDecimal("47.500000"), futuro, null, null)
        ));

        when(disponibilidadInsumoService.resolverAlmacenesPreferidos(insumo)).thenReturn(List.of(5L));

        DistribucionFefoResult preview = DistribucionFefoResult.builder()
                .productoInsumoId(200L)
                .requerido(new BigDecimal("350.000000"))
                .stockFisicoTotal(new BigDecimal("775.000000"))
                .stockReservadoTotal(new BigDecimal("472.500000"))
                .stockLibreTotal(new BigDecimal("302.500000"))
                .faltante(new BigDecimal("47.500000"))
                .suficiente(false)
                .almacenesPreferidos(List.of(5L))
                .build();

        when(disponibilidadInsumoService.calcularDisponibilidad(eq(200L), any(BigDecimal.class), eq(List.of(5L)), eq(true)))
                .thenReturn(preview);

        FormulaProductoResponse respuesta = service.obtenerFormulaActivaPorProducto(100L, new BigDecimal("700"));

        assertThat(respuesta.disponibilidadSuficiente).isFalse();
        assertThat(respuesta.detalles).hasSize(1);

        DetalleFormulaResponse detalleDto = respuesta.detalles.get(0);
        assertThat(detalleDto.cantidadTotalNecesaria).isEqualByComparingTo(new BigDecimal("350.0"));
        assertThat(detalleDto.estadoStock).isEqualTo("INSUFICIENTE");
        assertThat(detalleDto.stockLibreFefo).isEqualByComparingTo(new BigDecimal("302.500000"));
        assertThat(detalleDto.stockDisponible).isEqualByComparingTo(detalleDto.stockLibreFefo);
        assertThat(detalleDto.faltanteFefo).isEqualByComparingTo(new BigDecimal("47.500000"));
        assertThat(detalleDto.maxProducible).isEqualTo(605);
        assertThat(detalleDto.maximoProducible).isEqualTo(605);
        assertThat(detalleDto.unidadInsumoSimbolo).isEqualTo("MILILITRO");
        assertThat(detalleDto.unidadInsumoNombre).isEqualTo("MILILITRO");
        assertThat(detalleDto.unidadInsumoNombrePlural).isEqualTo("MILILITROS");
        assertThat(detalleDto.unidadProductoFabricableSimbolo).isEqualTo("UND");
        assertThat(detalleDto.unidadProductoFabricableNombre).isEqualTo("UNIDAD");
        assertThat(detalleDto.unidadProductoFabricableNombrePlural).isEqualTo("UNIDADES");
        assertThat(detalleDto.bloqueante.isInsuficiente()).isTrue();
        assertThat(detalleDto.bloqueante.getMotivo()).isEqualTo("RETENIDO");
    }

    @Test
    @DisplayName("obtenerFormulaActivaPorProducto marca suficiente cuando stock libre FEFO cubre requerimiento")
    void obtenerFormulaActivaPorProducto_stockSuficiente() {
        Producto producto = new Producto();
        producto.setId(101);
        UnidadMedida um = new UnidadMedida();
        um.setSimbolo("UND");
        um.setNombre("UNIDAD");
        um.setNombrePlural("UNIDADES");
        producto.setUnidadMedida(um);

        Producto insumo = new Producto();
        insumo.setId(300);
        insumo.setNombre("ACIDO ASCORBICO");
        UnidadMedida umInsumo = new UnidadMedida();
        umInsumo.setSimbolo("MILILITRO");
        umInsumo.setNombre("MILILITRO");
        umInsumo.setNombrePlural("MILILITROS");
        insumo.setUnidadMedida(umInsumo);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(insumo);
        detalle.setCantidadNecesaria(new BigDecimal("1.0"));

        FormulaProducto formula = new FormulaProducto();
        formula.setId(51L);
        formula.setProducto(producto);
        formula.setDetalles(List.of(detalle));

        when(formulaRepository.findByProductoIdAndEstadoAndActivoTrue(101L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(loteProductoRepository.sumarPorEstado(300L)).thenReturn(List.of());
        when(loteProductoRepository.listarLotesPorProducto(300L)).thenReturn(List.of());
        when(disponibilidadInsumoService.resolverAlmacenesPreferidos(insumo)).thenReturn(List.of());

        DistribucionFefoResult preview = DistribucionFefoResult.builder()
                .productoInsumoId(300L)
                .requerido(new BigDecimal("50.000000"))
                .stockFisicoTotal(new BigDecimal("120.000000"))
                .stockReservadoTotal(BigDecimal.ZERO)
                .stockLibreTotal(new BigDecimal("120.000000"))
                .faltante(BigDecimal.ZERO)
                .suficiente(true)
                .build();

        when(disponibilidadInsumoService.calcularDisponibilidad(eq(300L), any(BigDecimal.class), eq(List.of()), eq(true)))
                .thenReturn(preview);

        FormulaProductoResponse respuesta = service.obtenerFormulaActivaPorProducto(101L, new BigDecimal("50"));

        assertThat(respuesta.disponibilidadSuficiente).isTrue();
        DetalleFormulaResponse detalleDto = respuesta.detalles.get(0);
        assertThat(detalleDto.estadoStock).isEqualTo("SUFICIENTE");
        assertThat(detalleDto.stockLibreFefo).isEqualByComparingTo(new BigDecimal("120.000000"));
        assertThat(detalleDto.maxProducible).isEqualTo(120);
        assertThat(detalleDto.maximoProducible).isEqualTo(120);
        assertThat(detalleDto.unidadProductoFabricableSimbolo).isEqualTo("UND");
        assertThat(detalleDto.bloqueante.isInsuficiente()).isFalse();
    }

    @Test
    @DisplayName("obtenerFormulaActivaPorProducto expone unidades separadas de insumo y producto fabricable por detalle")
    void obtenerFormulaActivaPorProducto_exponeUnidadesSeparadas() {
        Producto producto = new Producto();
        producto.setId(102);
        UnidadMedida unidadProducto = new UnidadMedida();
        unidadProducto.setSimbolo("UND");
        unidadProducto.setNombre("UNIDAD");
        unidadProducto.setNombrePlural("UNIDADES");
        producto.setUnidadMedida(unidadProducto);

        Producto insumoGr = new Producto();
        insumoGr.setId(401);
        UnidadMedida unidadGr = new UnidadMedida();
        unidadGr.setSimbolo("GR");
        unidadGr.setNombre("GRAMO");
        unidadGr.setNombrePlural("GRAMOS");
        insumoGr.setUnidadMedida(unidadGr);

        Producto insumoUnd = new Producto();
        insumoUnd.setId(402);
        UnidadMedida unidadUnd = new UnidadMedida();
        unidadUnd.setSimbolo("UND");
        unidadUnd.setNombre("UNIDAD");
        unidadUnd.setNombrePlural("UNIDADES");
        insumoUnd.setUnidadMedida(unidadUnd);

        Producto insumoMl = new Producto();
        insumoMl.setId(403);
        UnidadMedida unidadMl = new UnidadMedida();
        unidadMl.setSimbolo("ML");
        unidadMl.setNombre("MILILITRO");
        unidadMl.setNombrePlural("MILILITROS");
        insumoMl.setUnidadMedida(unidadMl);

        DetalleFormula detalleGr = new DetalleFormula();
        detalleGr.setInsumo(insumoGr);
        detalleGr.setCantidadNecesaria(new BigDecimal("2"));

        DetalleFormula detalleUnd = new DetalleFormula();
        detalleUnd.setInsumo(insumoUnd);
        detalleUnd.setCantidadNecesaria(new BigDecimal("2"));

        DetalleFormula detalleMl = new DetalleFormula();
        detalleMl.setInsumo(insumoMl);
        detalleMl.setCantidadNecesaria(new BigDecimal("2"));

        FormulaProducto formula = new FormulaProducto();
        formula.setId(52L);
        formula.setProducto(producto);
        formula.setDetalles(List.of(detalleGr, detalleUnd, detalleMl));

        when(formulaRepository.findByProductoIdAndEstadoAndActivoTrue(102L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(loteProductoRepository.sumarPorEstado(anyLong())).thenReturn(List.of());
        when(loteProductoRepository.listarLotesPorProducto(anyLong())).thenReturn(List.of());
        when(disponibilidadInsumoService.resolverAlmacenesPreferidos(any(Producto.class))).thenReturn(List.of());
        when(disponibilidadInsumoService.calcularDisponibilidad(anyLong(), any(BigDecimal.class), eq(List.of()), eq(true)))
                .thenReturn(DistribucionFefoResult.builder()
                        .stockLibreTotal(new BigDecimal("10"))
                        .faltante(BigDecimal.ZERO)
                        .suficiente(true)
                        .build());

        FormulaProductoResponse respuesta = service.obtenerFormulaActivaPorProducto(102L, BigDecimal.ONE);

        assertThat(respuesta.detalles).hasSize(3);

        DetalleFormulaResponse casoA = respuesta.detalles.get(0);
        assertThat(casoA.unidadInsumoSimbolo).isEqualTo("GR");
        assertThat(casoA.unidadProductoFabricableSimbolo).isEqualTo("UND");
        assertThat(casoA.unidadProductoFabricableNombrePlural).isEqualTo("UNIDADES");
        assertThat(casoA.maximoProducible).isEqualTo(5);

        DetalleFormulaResponse casoB = respuesta.detalles.get(1);
        assertThat(casoB.unidadInsumoSimbolo).isEqualTo("UND");
        assertThat(casoB.unidadProductoFabricableSimbolo).isEqualTo("UND");
        assertThat(casoB.maximoProducible).isEqualTo(5);

        DetalleFormulaResponse casoC = respuesta.detalles.get(2);
        assertThat(casoC.unidadInsumoSimbolo).isEqualTo("ML");
        assertThat(casoC.unidadProductoFabricableSimbolo).isEqualTo("UND");
        assertThat(casoC.maximoProducible).isEqualTo(5);
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
        origen.setVersionMajor(1);
        origen.setVersionMinor(9);
        origen.setVersion("V1.9");
        origen.setObservacion("Observación previa");
        origen.setDetalles(List.of(detalleOriginal));
        origen.setDocumentos(List.of(documentoOriginal));
        detalleOriginal.setFormula(origen);
        documentoOriginal.setFormula(origen);

        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setNombreCompleto("Usuario Clonador");

        when(formulaRepository.findById(7L)).thenReturn(Optional.of(origen));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));
        when(formulaRepository.findTopByProductoIdOrderByVersionMajorDescVersionMinorDesc(5L))
                .thenReturn(Optional.of(origen));
        when(formulaRepository.save(any(FormulaProducto.class))).thenAnswer(invocation -> {
            FormulaProducto guardado = invocation.getArgument(0);
            guardado.setId(11L);
            return guardado;
        });

        FormulaProducto clon = service.clonarFormula(7L, 3L);

        assertThat(clon.getId()).isEqualTo(11L);
        assertThat(clon.getProducto()).isEqualTo(producto);
        assertThat(clon.getVersion()).isEqualTo("V2.0");
        assertThat(clon.getVersionMajor()).isEqualTo(2);
        assertThat(clon.getVersionMinor()).isEqualTo(0);
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
    @DisplayName("versionado calcula y formatea la siguiente versión con rollover")
    void versionadoCalculaSiguienteVersion() {
        FormulaProductoServiceImpl.VersionParts actual = FormulaProductoServiceImpl.parsearVersion("V3.9");
        FormulaProductoServiceImpl.VersionParts siguiente = FormulaProductoServiceImpl.calcularSiguienteVersion(actual);

        assertThat(FormulaProductoServiceImpl.formatearVersion(siguiente)).isEqualTo("V4.0");
        assertThat(siguiente.major()).isEqualTo(4);
        assertThat(siguiente.minor()).isEqualTo(0);
    }

    @Test
    @DisplayName("versionado formatea versión cuando solo existe el string")
    void versionadoFormateaVersion() {
        FormulaProductoServiceImpl.VersionParts version = FormulaProductoServiceImpl.parsearVersion("V11.2");

        assertThat(FormulaProductoServiceImpl.formatearVersion(version)).isEqualTo("V11.2");
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
