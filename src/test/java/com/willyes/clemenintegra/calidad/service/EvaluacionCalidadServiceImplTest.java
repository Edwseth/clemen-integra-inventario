package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.mapper.EvaluacionCalidadMapper;
import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.enums.EstadoEvaluacionCalidad;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void obtieneEstadoEvaluacionConsolidado() {
        lote.getProducto().setRequiereAnalisisFisico(true);

        EvaluacionCalidad evalFisico = EvaluacionCalidad.builder()
                .id(70L)
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .usuarioEvaluador(evaluador)
                .loteProducto(lote)
                .fechaEvaluacion(LocalDateTime.now())
                .build();

        EvaluacionCalidad evalQuimicoMicro = EvaluacionCalidad.builder()
                .id(71L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .usuarioEvaluador(evaluador)
                .loteProducto(lote)
                .fechaEvaluacion(LocalDateTime.now())
                .build();

        when(repository.findAllWithinFechaEvaluacion(any(), any()))
                .thenReturn(List.of(evalFisico, evalQuimicoMicro));

        ResultadoAnalisisMicrobiologico resultadoMicro = ResultadoAnalisisMicrobiologico.builder()
                .id(200L)
                .evaluacion(evalQuimicoMicro)
                .build();

        when(resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(any()))
                .thenReturn(List.of(resultadoMicro));

        var consolidados = service.obtenerEvaluacionesConsolidadas(evalFisico.getFechaEvaluacion().toLocalDate(),
                evalFisico.getFechaEvaluacion().toLocalDate());

        assertThat(consolidados).hasSize(1);
        assertThat(consolidados.get(0).getEstadoEvaluacion()).isEqualTo(EstadoEvaluacionCalidad.EVALUADO);
    }
}
