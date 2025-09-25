package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ArchivoEvaluacionDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.mapper.EvaluacionCalidadMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluacionCalidadServiceImplTest {

    @Mock
    private EvaluacionCalidadRepository repository;
    @Mock
    private LoteProductoRepository loteRepository;
    @Mock
    private EvaluacionCalidadMapper mapper;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InventoryCatalogResolver catalogResolver;
    @Mock
    private AlmacenRepository almacenRepository;

    private EvaluacionCalidadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvaluacionCalidadServiceImpl(repository, loteRepository, mapper,
                usuarioService, usuarioRepository, catalogResolver, almacenRepository);
    }

    @Test
    void registrarEvaluacionMicroNoModificaAlmacen() throws Exception {
        Usuario user = buildUsuario(1L, RolUsuario.ROL_MICROBIOLOGO);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(user);

        LoteProducto lote = buildLoteEnCuarentena(5L, 7);
        when(loteRepository.findById(5L)).thenReturn(Optional.of(lote), Optional.of(lote));
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(repository.existsByLoteProductoIdAndTipoEvaluacion(5L, TipoEvaluacion.MICROBIOLOGICO)).thenReturn(false);
        mockMapperToEntity();
        mockMapperToResponse();
        when(repository.save(any(EvaluacionCalidad.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MultipartFile archivo = buildArchivoMock();
        EvaluacionCalidadRequestDTO dto = baseRequest(5L, TipoEvaluacion.MICROBIOLOGICO);

        service.crear(dto, List.of(archivo));

        verify(loteRepository, never()).saveAndFlush(any());
        assertThat(lote.getAlmacen().getId()).isEqualTo(7);
    }

    @Test
    void registrarEvaluacionFisicoQuimicoNoModificaAlmacen() throws Exception {
        Usuario user = buildUsuario(2L, RolUsuario.ROL_ANALISTA_CALIDAD);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(user);

        LoteProducto lote = buildLoteEnCuarentena(8L, 7);
        when(loteRepository.findById(8L)).thenReturn(Optional.of(lote), Optional.of(lote));
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(repository.existsByLoteProductoIdAndTipoEvaluacion(8L, TipoEvaluacion.FISICO_QUIMICO)).thenReturn(false);
        mockMapperToEntity();
        mockMapperToResponse();
        when(repository.save(any(EvaluacionCalidad.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MultipartFile archivo = buildArchivoMock();
        EvaluacionCalidadRequestDTO dto = baseRequest(8L, TipoEvaluacion.FISICO_QUIMICO);

        service.crear(dto, List.of(archivo));

        verify(loteRepository, never()).saveAndFlush(any());
        assertThat(lote.getAlmacen().getId()).isEqualTo(7);
    }

    @Test
    void registrarEvaluacionMicroRestauraSiOtroProcesoMueveElLote() throws Exception {
        Usuario user = buildUsuario(3L, RolUsuario.ROL_MICROBIOLOGO);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(user);

        LoteProducto loteInicial = buildLoteEnCuarentena(15L, 7);
        LoteProducto loteMutado = buildLoteEnCuarentena(15L, 1);
        when(loteRepository.findById(15L)).thenReturn(Optional.of(loteInicial), Optional.of(loteMutado));
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(almacenRepository.findById(7L)).thenReturn(Optional.of(new Almacen(7)));
        when(repository.existsByLoteProductoIdAndTipoEvaluacion(15L, TipoEvaluacion.MICROBIOLOGICO)).thenReturn(false);
        mockMapperToEntity();
        mockMapperToResponse();
        when(repository.save(any(EvaluacionCalidad.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MultipartFile archivo = buildArchivoMock();
        EvaluacionCalidadRequestDTO dto = baseRequest(15L, TipoEvaluacion.MICROBIOLOGICO);

        service.crear(dto, List.of(archivo));

        ArgumentCaptor<LoteProducto> captor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getAlmacen().getId()).isEqualTo(7);
    }

    @Test
    void registrarEvaluacionMicroCorrigeAlmacenSiYaNoEstaEnCuarentena() throws Exception {
        Usuario user = buildUsuario(4L, RolUsuario.ROL_MICROBIOLOGO);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(user);

        LoteProducto loteFuera = buildLoteEnCuarentena(20L, 1);
        when(loteRepository.findById(20L)).thenReturn(Optional.of(loteFuera), Optional.of(loteFuera));
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(almacenRepository.findById(7L)).thenReturn(Optional.of(new Almacen(7)));
        when(repository.existsByLoteProductoIdAndTipoEvaluacion(20L, TipoEvaluacion.MICROBIOLOGICO)).thenReturn(false);
        mockMapperToEntity();
        mockMapperToResponse();
        when(repository.save(any(EvaluacionCalidad.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MultipartFile archivo = buildArchivoMock();
        EvaluacionCalidadRequestDTO dto = baseRequest(20L, TipoEvaluacion.MICROBIOLOGICO);

        service.crear(dto, List.of(archivo));

        verify(loteRepository).saveAndFlush(loteFuera);
        assertThat(loteFuera.getAlmacen().getId()).isEqualTo(7);
    }

    private void mockMapperToEntity() {
        when(mapper.toEntity(any(), any(), any())).thenAnswer(invocation -> {
            EvaluacionCalidadRequestDTO dto = invocation.getArgument(0);
            LoteProducto lote = invocation.getArgument(1);
            Usuario user = invocation.getArgument(2);
            return EvaluacionCalidad.builder()
                    .loteProducto(lote)
                    .usuarioEvaluador(user)
                    .tipoEvaluacion(dto.getTipoEvaluacion())
                    .resultado(dto.getResultado())
                    .observaciones(dto.getObservaciones())
                    .build();
        });
    }

    private void mockMapperToResponse() {
        when(mapper.toResponseDTO(any())).thenReturn(new com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO());
    }

    private Usuario buildUsuario(Long id, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        return usuario;
    }

    private LoteProducto buildLoteEnCuarentena(Long id, Integer almacenId) {
        LoteProducto lote = new LoteProducto();
        lote.setId(id);
        lote.setEstado(com.willyes.clemenintegra.inventario.model.enums.EstadoLote.EN_CUARENTENA);
        lote.setAlmacen(new Almacen(almacenId));
        lote.setProducto(new com.willyes.clemenintegra.inventario.model.Producto());
        return lote;
    }

    private MultipartFile buildArchivoMock() throws Exception {
        MultipartFile archivo = mock(MultipartFile.class);
        when(archivo.isEmpty()).thenReturn(false);
        when(archivo.getOriginalFilename()).thenReturn("resultado.pdf");
        doNothing().when(archivo).transferTo(any(java.io.File.class));
        return archivo;
    }

    private EvaluacionCalidadRequestDTO baseRequest(Long loteId, TipoEvaluacion tipo) {
        return EvaluacionCalidadRequestDTO.builder()
                .loteProductoId(loteId)
                .tipoEvaluacion(tipo)
                .resultado(ResultadoEvaluacion.CONFORME)
                .observaciones("ok")
                .archivosAdjuntos(List.of(ArchivoEvaluacionDTO.builder().nombreVisible("doc").build()))
                .build();
    }
}
