package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.calidad.service.VidaUtilProductoService;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.ReservaLoteService;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.repository.*;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class OrdenProduccionServiceChecklistTest {

    private OrdenProduccionServiceImpl service;
    private ChecklistEtapaService checklistEtapaService;

    @BeforeEach
    void setUp() {
        FormulaProductoRepository formulaProductoRepository = mock(FormulaProductoRepository.class);
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        SolicitudMovimientoService solicitudMovimientoService = mock(SolicitudMovimientoService.class);
        OrdenProduccionRepository ordenProduccionRepository = mock(OrdenProduccionRepository.class);
        MotivoMovimientoRepository motivoMovimientoRepository = mock(MotivoMovimientoRepository.class);
        TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository = mock(TipoMovimientoDetalleRepository.class);
        CierreProduccionRepository cierreProduccionRepository = mock(CierreProduccionRepository.class);
        MovimientoInventarioService movimientoInventarioService = mock(MovimientoInventarioService.class);
        LoteProductoRepository loteProductoRepository = mock(LoteProductoRepository.class);
        AlmacenRepository almacenRepository = mock(AlmacenRepository.class);
        UnidadConversionService unidadConversionService = mock(UnidadConversionService.class);
        EtapaProduccionRepository etapaProduccionRepository = mock(EtapaProduccionRepository.class);
        EtapaPlantillaRepository etapaPlantillaRepository = mock(EtapaPlantillaRepository.class);
        MovimientoInventarioRepository movimientoInventarioRepository = mock(MovimientoInventarioRepository.class);
        com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper movimientoInventarioMapper = mock(com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper.class);
        com.willyes.clemenintegra.shared.service.UsuarioService usuarioService = mock(com.willyes.clemenintegra.shared.service.UsuarioService.class);
        SolicitudMovimientoRepository solicitudMovimientoRepository = mock(SolicitudMovimientoRepository.class);
        InventoryCatalogResolver catalogResolver = mock(InventoryCatalogResolver.class);
        com.willyes.clemenintegra.inventario.service.UmValidator umValidator = mock(com.willyes.clemenintegra.inventario.service.UmValidator.class);
        VidaUtilProductoService vidaUtilProductoService = mock(VidaUtilProductoService.class);
        ReservaLoteService reservaLoteService = mock(ReservaLoteService.class);
        ReservaLoteRepository reservaLoteRepository = mock(ReservaLoteRepository.class);
        DisponibilidadInsumoService disponibilidadInsumoService = mock(DisponibilidadInsumoService.class);
        checklistEtapaService = mock(ChecklistEtapaService.class);
        com.willyes.clemenintegra.produccion.repository.ChecklistEtapaItemRepository checklistEtapaItemRepository =
                mock(com.willyes.clemenintegra.produccion.repository.ChecklistEtapaItemRepository.class);
        LoteConsecutivoDiaService loteConsecutivoDiaService = mock(LoteConsecutivoDiaService.class);
        OpHomeopaticoOverrideRepository opHomeopaticoOverrideRepository = mock(OpHomeopaticoOverrideRepository.class);

        service = new OrdenProduccionServiceImpl(
                formulaProductoRepository,
                productoRepository,
                usuarioRepository,
                solicitudMovimientoService,
                ordenProduccionRepository,
                motivoMovimientoRepository,
                tipoMovimientoDetalleRepository,
                cierreProduccionRepository,
                movimientoInventarioService,
                loteProductoRepository,
                almacenRepository,
                unidadConversionService,
                etapaProduccionRepository,
                etapaPlantillaRepository,
                movimientoInventarioRepository,
                movimientoInventarioMapper,
                usuarioService,
                solicitudMovimientoRepository,
                catalogResolver,
                umValidator,
                vidaUtilProductoService,
                reservaLoteService,
                reservaLoteRepository,
                disponibilidadInsumoService,
                checklistEtapaService,
                checklistEtapaItemRepository,
                loteConsecutivoDiaService,
                opHomeopaticoOverrideRepository
        );

        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion.EN_PROCESO)
                .build();
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(2L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now().minusHours(1))
                .build();
        when(etapaProduccionRepository.findById(2L)).thenReturn(Optional.of(etapa));
        when(ordenProduccionRepository.save(orden)).thenReturn(orden);
    }

    @Test
    @DisplayName("Finalizar etapa falla si checklist obligatorio incompleto")
    void finalizarEtapaChecklistIncompleto() {

        doThrow(new CustomBusinessException(
                com.willyes.clemenintegra.shared.exception.ApiErrorCode.CHECKLIST_ETAPA_INCOMPLETO,
                "CHECKLIST_ETAPA_INCOMPLETO"
        )).when(checklistEtapaService).validarChecklistCompleto(anyLong());

        assertThrows(CustomBusinessException.class, () -> service.finalizarEtapa(1L, 2L, 3L));
    }

}
