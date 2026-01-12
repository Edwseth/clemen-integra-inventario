package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionConsolidadaListadoDTO;
import com.willyes.clemenintegra.calidad.mapper.EvaluacionCalidadMapper;
import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.ResultadoAnalisisMicrobiologicoRepository;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluacionCalidadServiceImplTest {

    @Mock
    private EvaluacionCalidadRepository repository;
    @Mock
    private LoteProductoRepository loteRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InventoryCatalogResolver catalogResolver;
    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private CondicionUsoService condicionUsoService;
    @Mock
    private RetencionLoteService retencionLoteService;
    @Mock
    private NoConformidadService noConformidadService;
    @Mock
    private ResultadoAnalisisMicroService resultadoAnalisisMicroService;
    @Mock
    private ResultadoAnalisisMicrobiologicoRepository resultadoAnalisisMicrobiologicoRepository;

    @Spy
    private EvaluacionCalidadMapper mapper = new EvaluacionCalidadMapper();

    @InjectMocks
    private EvaluacionCalidadServiceImpl service;

    private Usuario evaluador;
    private LoteProducto lote;

    @BeforeEach
    void setUp() {
        evaluador = new Usuario();
        evaluador.setId(99L);
        evaluador.setRol(RolUsuario.ROL_MICROBIOLOGO);
        evaluador.setNombreCompleto("Micro Biólogo");

        Producto producto = new Producto();
        producto.setId(1);
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);

        Almacen almacen = new Almacen(5);

        lote = new LoteProducto();
        lote.setId(10L);
        lote.setProducto(producto);
        lote.setEstado(EstadoLote.EN_CUARENTENA);
        lote.setAlmacen(almacen);

        lenient().when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(evaluador);
        lenient().when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(5L);
        lenient().when(loteRepository.findById(10L)).thenReturn(Optional.of(lote));
    }

    @Test
    void creaNuevaEvaluacionQMCuandoNoExiste() {
        EvaluacionCalidadRequestDTO dto = EvaluacionCalidadRequestDTO.builder()
                .loteProductoId(10L)
                .usuarioEvaluadorId(99L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .observaciones("OK")
                .build();

        when(repository.findFirstByLoteProductoIdAndTipoEvaluacion(10L, TipoEvaluacion.QUIMICO_MICROBIOLOGICO))
                .thenReturn(Optional.empty());
        when(repository.save(any(EvaluacionCalidad.class))).thenAnswer(invocation -> {
            EvaluacionCalidad eval = invocation.getArgument(0);
            eval.setId(50L);
            return eval;
        });

        var respuesta = service.crear(dto, null);

        assertThat(respuesta.getId()).isEqualTo(50L);
        assertThat(respuesta.getTipoEvaluacion()).isEqualTo(TipoEvaluacion.QUIMICO_MICROBIOLOGICO);
        assertThat(respuesta.getResultado()).isEqualTo(ResultadoEvaluacion.CONFORME);
    }

    @Test
    void reutilizaEvaluacionQMExistente() {
        EvaluacionCalidad existente = EvaluacionCalidad.builder()
                .id(60L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .observaciones("Inicial")
                .resultado(ResultadoEvaluacion.CONDICIONADO)
                .loteProducto(lote)
                .usuarioEvaluador(evaluador)
                .fechaEvaluacion(LocalDateTime.now().minusDays(1))
                .archivosAdjuntos(new ArrayList<>(List.of(ArchivoEvaluacion.builder()
                        .nombreVisible("Químico")
                        .nombreArchivo("quim.pdf")
                        .build())))
                .build();

        when(repository.findFirstByLoteProductoIdAndTipoEvaluacion(10L, TipoEvaluacion.QUIMICO_MICROBIOLOGICO))
                .thenReturn(Optional.of(existente));
        when(repository.save(any(EvaluacionCalidad.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvaluacionCalidadRequestDTO dto = EvaluacionCalidadRequestDTO.builder()
                .loteProductoId(10L)
                .usuarioEvaluadorId(99L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .observaciones("Actualizada")
                .build();

        var respuesta = service.crear(dto, null);

        assertThat(respuesta.getId()).isEqualTo(60L);
        assertThat(existente.getObservaciones()).isEqualTo("Actualizada");
        assertThat(existente.getArchivosAdjuntos())
                .extracting(ArchivoEvaluacion::getNombreVisible)
                .contains("Químico");
    }

    @Test
    void consolidaFqmConDisciplinasConformes() {
        lote.getProducto().setRequiereAnalisisFisico(true);

        EvaluacionCalidad evalFisico = EvaluacionCalidad.builder()
                .id(70L)
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .usuarioEvaluador(evaluador)
                .loteProducto(lote)
                .fechaEvaluacion(LocalDateTime.now())
                .archivosAdjuntos(List.of(ArchivoEvaluacion.builder()
                        .nombreVisible("Fisico")
                        .nombreArchivo("fisico.pdf")
                        .build()))
                .build();

        EvaluacionCalidad evalQuimicoMicro = EvaluacionCalidad.builder()
                .id(71L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .usuarioEvaluador(evaluador)
                .loteProducto(lote)
                .fechaEvaluacion(LocalDateTime.now())
                .archivosAdjuntos(List.of(
                        ArchivoEvaluacion.builder()
                                .nombreVisible("Químico")
                                .nombreArchivo("quim.pdf")
                                .build(),
                        ArchivoEvaluacion.builder()
                                .nombreVisible(com.willyes.clemenintegra.calidad.service.ArchivoEvaluacionConstants.NOMBRE_VISIBLE_MICRO)
                                .nombreArchivo("micro.pdf")
                                .build()))
                .build();

        EvaluacionConsolidadaListadoDTO consolidado = new EvaluacionConsolidadaListadoDTO(
                71L,
                lote.getId(),
                evalFisico.getFechaEvaluacion(),
                "LOT-10",
                "Producto Q",
                ResultadoEvaluacion.CONFORME,
                TipoEvaluacion.QUIMICO_MICROBIOLOGICO,
                evaluador.getNombreCompleto(),
                com.willyes.clemenintegra.calidad.model.enums.EstadoEvaluacionCalidad.EVALUADO,
                EstadoLote.EN_CUARENTENA,
                2L);

        when(repository.findConsolidadoListado(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(consolidado)));

        var consolidados = service.obtenerEvaluacionesConsolidadas(evalFisico.getFechaEvaluacion().toLocalDate(),
                evalFisico.getFechaEvaluacion().toLocalDate(), PageRequest.of(0, 10));

        assertThat(consolidados).hasSize(1);
        var dto = consolidados.getContent().get(0);
        assertThat(dto.getLoteId()).isEqualTo(lote.getId());
        assertThat(dto.getCodigoLote()).isEqualTo("LOT-10");
        assertThat(dto.getNombreProducto()).isEqualTo("Producto Q");
        assertThat(dto.getTipoEvaluacion()).isEqualTo(TipoEvaluacion.QUIMICO_MICROBIOLOGICO);
        assertThat(dto.getCantidadAdjuntos()).isEqualTo(2L);
        assertThat(dto.isTieneAdjuntos()).isTrue();
    }

    @Test
    void consolidaMicroPendienteCuandoNoHayResultados() {
        Producto productoSoloMicro = new Producto();
        productoSoloMicro.setId(2);
        productoSoloMicro.setRequiereAnalisisMicrobiologico(true);
        LoteProducto loteSoloMicro = new LoteProducto();
        loteSoloMicro.setId(30L);
        loteSoloMicro.setProducto(productoSoloMicro);
        loteSoloMicro.setEstado(EstadoLote.EN_CUARENTENA);

        EvaluacionCalidad evalMicro = EvaluacionCalidad.builder()
                .id(80L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .usuarioEvaluador(evaluador)
                .loteProducto(loteSoloMicro)
                .fechaEvaluacion(LocalDateTime.now())
                .build();

        EvaluacionConsolidadaListadoDTO consolidado = new EvaluacionConsolidadaListadoDTO(
                80L,
                loteSoloMicro.getId(),
                evalMicro.getFechaEvaluacion(),
                "LOT-30",
                "Producto Micro",
                ResultadoEvaluacion.CONFORME,
                TipoEvaluacion.QUIMICO_MICROBIOLOGICO,
                evaluador.getNombreCompleto(),
                com.willyes.clemenintegra.calidad.model.enums.EstadoEvaluacionCalidad.EVALUADO,
                EstadoLote.EN_CUARENTENA,
                0L);
        when(repository.findConsolidadoListado(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(consolidado)));

        var consolidados = service.obtenerEvaluacionesConsolidadas(evalMicro.getFechaEvaluacion().toLocalDate(),
                evalMicro.getFechaEvaluacion().toLocalDate(), PageRequest.of(0, 10));

        assertThat(consolidados).hasSize(1);
        var dto = consolidados.getContent().get(0);
        assertThat(dto.getLoteId()).isEqualTo(loteSoloMicro.getId());
        assertThat(dto.getCodigoLote()).isEqualTo("LOT-30");
        assertThat(dto.isTieneAdjuntos()).isFalse();
    }

    @Test
    void consolidaMicroNoRequerido() {
        Producto productoSinMicro = new Producto();
        productoSinMicro.setId(3);
        productoSinMicro.setRequiereAnalisisMicrobiologico(false);
        LoteProducto loteSinMicro = new LoteProducto();
        loteSinMicro.setId(40L);
        loteSinMicro.setProducto(productoSinMicro);
        loteSinMicro.setEstado(EstadoLote.EN_CUARENTENA);

        EvaluacionCalidad evalFisico = EvaluacionCalidad.builder()
                .id(90L)
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .usuarioEvaluador(evaluador)
                .loteProducto(loteSinMicro)
                .fechaEvaluacion(LocalDateTime.now())
                .build();

        EvaluacionConsolidadaListadoDTO consolidado = new EvaluacionConsolidadaListadoDTO(
                90L,
                loteSinMicro.getId(),
                evalFisico.getFechaEvaluacion(),
                "LOT-40",
                "Producto F",
                ResultadoEvaluacion.CONFORME,
                TipoEvaluacion.FISICO,
                evaluador.getNombreCompleto(),
                com.willyes.clemenintegra.calidad.model.enums.EstadoEvaluacionCalidad.EVALUADO,
                EstadoLote.EN_CUARENTENA,
                1L);
        when(repository.findConsolidadoListado(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(consolidado)));

        var consolidados = service.obtenerEvaluacionesConsolidadas(evalFisico.getFechaEvaluacion().toLocalDate(),
                evalFisico.getFechaEvaluacion().toLocalDate(), PageRequest.of(0, 10));

        assertThat(consolidados).hasSize(1);
        var dto = consolidados.getContent().get(0);
        assertThat(dto.getLoteId()).isEqualTo(loteSinMicro.getId());
        assertThat(dto.getCodigoLote()).isEqualTo("LOT-40");
        assertThat(dto.isTieneAdjuntos()).isTrue();
    }

    @Test
    void detalleEvaluacionFisicaMarcaEstados() {
        Producto producto = new Producto();
        producto.setId(10);
        producto.setRequiereAnalisisFisico(true);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);

        LoteProducto loteDetalle = new LoteProducto();
        loteDetalle.setId(100L);
        loteDetalle.setCodigoLote("LOT-FIS");
        loteDetalle.setProducto(producto);

        EvaluacionCalidad evalFisico = EvaluacionCalidad.builder()
                .id(300L)
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .usuarioEvaluador(evaluador)
                .loteProducto(loteDetalle)
                .fechaEvaluacion(LocalDateTime.now())
                .build();

        when(repository.findById(300L)).thenReturn(Optional.of(evalFisico));
        when(repository.findByLoteProductoId(100L)).thenReturn(List.of(evalFisico));
        when(resultadoAnalisisMicroService.obtenerPorEvaluacion(300L)).thenReturn(List.of());

        var detalle = service.obtenerDetalle(300L);

        assertThat(detalle.getEstadoFisico()).isEqualTo("EVALUADO");
        assertThat(detalle.getEstadoQuimicoMicrobiologico()).isEqualTo("NO_REQUERIDO");
        assertThat(detalle.getEstadoMicrobiologico()).isEqualTo("NO_REQUERIDO");
    }

    @Test
    void detalleEvaluacionMarcaMicroPendienteSinResultados() {
        Producto producto = new Producto();
        producto.setId(11);
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);

        LoteProducto loteDetalle = new LoteProducto();
        loteDetalle.setId(110L);
        loteDetalle.setCodigoLote("LOT-QM");
        loteDetalle.setProducto(producto);

        EvaluacionCalidad evalQM = EvaluacionCalidad.builder()
                .id(310L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .usuarioEvaluador(evaluador)
                .loteProducto(loteDetalle)
                .fechaEvaluacion(LocalDateTime.now())
                .build();

        when(repository.findById(310L)).thenReturn(Optional.of(evalQM));
        when(repository.findByLoteProductoId(110L)).thenReturn(List.of(evalQM));
        when(resultadoAnalisisMicroService.obtenerPorEvaluacion(310L)).thenReturn(List.of());
        when(resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(List.of(310L)))
                .thenReturn(List.of());

        var detalle = service.obtenerDetalle(310L);

        assertThat(detalle.getEstadoQuimicoMicrobiologico()).isEqualTo("EVALUADO");
        assertThat(detalle.getEstadoMicrobiologico()).isEqualTo("PENDIENTE");
    }

    @Test
    void generaExcelEvaluacionesConFilaDeDatos() throws Exception {
        Producto producto = new Producto();
        producto.setId(8);
        producto.setNombre("Producto Excel");
        producto.setTipoAnalisisCalidad(com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad.FISICO);

        LoteProducto loteExcel = new LoteProducto();
        loteExcel.setId(22L);
        loteExcel.setCodigoLote("LOT-EXCEL");
        loteExcel.setProducto(producto);
        loteExcel.setEstado(EstadoLote.EN_CUARENTENA);

        EvaluacionCalidad eval = new EvaluacionCalidad();
        eval.setId(11L);
        eval.setFechaEvaluacion(LocalDateTime.now());
        eval.setResultado(ResultadoEvaluacion.CONFORME);
        eval.setTipoEvaluacion(TipoEvaluacion.FISICO);
        eval.setLoteProducto(loteExcel);
        eval.setUsuarioEvaluador(evaluador);
        eval.setArchivosAdjuntos(List.of(ArchivoEvaluacion.builder().nombreArchivo("a.pdf").build()));

        when(repository.findAllWithRelations()).thenReturn(List.of(eval));
        when(noConformidadService.obtenerActivaPorLoteYEvaluacion(any(), any())).thenReturn(Optional.empty());

        byte[] excel = service.generarReporteEvaluacionesExcel(null, null, null);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            var sheet = workbook.getSheetAt(0);
            var header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Fecha evaluación");
            var dataRow = sheet.getRow(1);
            assertThat((Object) dataRow).isNotNull();
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("LOT-EXCEL");
            assertThat(dataRow.getCell(9).getStringCellValue()).isEqualTo("SI");
        }
    }
}
