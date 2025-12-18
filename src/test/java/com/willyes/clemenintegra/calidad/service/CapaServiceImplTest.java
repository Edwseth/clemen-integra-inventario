package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.CapaArchivoDescargaDTO;
import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.mapper.CapaMapper;
import com.willyes.clemenintegra.calidad.model.Capa;
import com.willyes.clemenintegra.calidad.model.CapaArchivo;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCapa;
import com.willyes.clemenintegra.calidad.model.enums.TipoCapa;
import com.willyes.clemenintegra.calidad.repository.CapaArchivoRepository;
import com.willyes.clemenintegra.calidad.repository.CapaRepository;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapaServiceImplTest {

    @Mock
    private CapaRepository capaRepository;
    @Mock
    private NoConformidadRepository noConformidadRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CapaArchivoRepository capaArchivoRepository;

    private CapaServiceImpl service;
    private final CapaMapper mapper = new CapaMapper();
    private String originalUserDir;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new CapaServiceImpl(capaRepository, noConformidadRepository, usuarioRepository, capaArchivoRepository, mapper);
        originalUserDir = System.getProperty("user.dir");
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.dir", originalUserDir);
    }

    @Test
    void creaCapaConEstadoPorDefecto() {
        CapaDTO dto = CapaDTO.builder()
                .noConformidadId(5L)
                .tipo(TipoCapa.CORRECTIVA)
                .responsableId(8L)
                .fechaInicio(LocalDateTime.now())
                .observaciones("obs")
                .build();

        when(noConformidadRepository.findById(5L)).thenReturn(Optional.of(NoConformidad.builder().id(5L).build()));
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(Usuario.builder().id(8L).build()));
        when(capaRepository.save(any(Capa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CapaDTO result = service.crear(dto);

        ArgumentCaptor<Capa> captor = ArgumentCaptor.forClass(Capa.class);
        verify(capaRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoCapa.ACTIVA);
        assertThat(captor.getValue().getFechaInicio()).isNotNull();
        assertThat(result.getNoConformidadId()).isEqualTo(5L);
    }

    @Test
    void creaCapaPersisteObservacionesYFechaLimite() {
        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime limite = inicio.plusDays(7);
        CapaDTO dto = CapaDTO.builder()
                .noConformidadId(5L)
                .tipo(TipoCapa.CORRECTIVA)
                .responsableId(8L)
                .fechaInicio(inicio)
                .fechaLimite(limite)
                .observaciones("Detalle de prueba")
                .build();

        NoConformidad noConformidad = NoConformidad.builder()
                .id(5L)
                .codigo("NC-123")
                .build();
        Usuario responsable = Usuario.builder()
                .id(8L)
                .nombreCompleto("Responsable QA")
                .build();

        when(noConformidadRepository.findById(5L)).thenReturn(Optional.of(noConformidad));
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(responsable));
        when(capaRepository.save(any(Capa.class))).thenAnswer(invocation -> {
            Capa guardada = invocation.getArgument(0);
            guardada.setId(33L);
            return guardada;
        });

        CapaDTO result = service.crear(dto);

        ArgumentCaptor<Capa> captor = ArgumentCaptor.forClass(Capa.class);
        verify(capaRepository).save(captor.capture());
        assertThat(captor.getValue().getObservaciones()).isEqualTo("Detalle de prueba");
        assertThat(captor.getValue().getFechaLimite()).isEqualTo(limite);
        assertThat(result.getObservaciones()).isEqualTo("Detalle de prueba");
        assertThat(result.getFechaLimite()).isEqualTo(limite);
        assertThat(result.getNoConformidadCodigo()).isEqualTo("NC-123");
        assertThat(result.getResponsableNombre()).isEqualTo("Responsable QA");
    }

    @Test
    void cerrarCapaActualizaEstadoYFecha() {
        Capa capa = Capa.builder()
                .id(10L)
                .estado(EstadoCapa.ACTIVA)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .noConformidad(NoConformidad.builder().id(1L).build())
                .responsable(Usuario.builder().id(2L).build())
                .tipo(TipoCapa.PREVENTIVA)
                .build();

        when(capaRepository.findById(10L)).thenReturn(Optional.of(capa));
        when(capaRepository.save(any(Capa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(capaArchivoRepository.findByCapa_Id(10L)).thenReturn(List.of());

        CapaDTO respuesta = service.cerrar(10L);

        ArgumentCaptor<Capa> captor = ArgumentCaptor.forClass(Capa.class);
        verify(capaRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoCapa.CERRADA);
        assertThat(captor.getValue().getFechaCierre()).isNotNull();
        assertThat(respuesta.getEstado()).isEqualTo(EstadoCapa.CERRADA);
        assertThat(respuesta.getFechaCierre()).isNotNull();
    }

    @Test
    void cerrarCapaIdempotenteNoDuplicaActualizacion() {
        LocalDateTime fechaCierre = LocalDateTime.now().minusDays(2);
        Capa capa = Capa.builder()
                .id(3L)
                .estado(EstadoCapa.CERRADA)
                .fechaCierre(fechaCierre)
                .fechaInicio(LocalDateTime.now().minusDays(5))
                .noConformidad(NoConformidad.builder().id(1L).build())
                .responsable(Usuario.builder().id(2L).build())
                .tipo(TipoCapa.CORRECTIVA)
                .build();

        when(capaRepository.findById(3L)).thenReturn(Optional.of(capa));
        when(capaArchivoRepository.findByCapa_Id(3L)).thenReturn(List.of());

        CapaDTO respuesta = service.cerrar(3L);

        verify(capaRepository, never()).save(any());
        assertThat(respuesta.getFechaCierre()).isEqualTo(fechaCierre);
    }

    @Test
    void adjuntarArchivoGuardaMetadatosYArchivo() throws Exception {
        System.setProperty("user.dir", tempDir.toString());

        Capa capa = Capa.builder()
                .id(4L)
                .estado(EstadoCapa.ACTIVA)
                .fechaInicio(LocalDateTime.now())
                .noConformidad(NoConformidad.builder().id(1L).build())
                .responsable(Usuario.builder().id(2L).build())
                .tipo(TipoCapa.CORRECTIVA)
                .build();

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "evidencia.txt",
                "text/plain",
                "contenido-prueba".getBytes()
        );

        when(capaRepository.findById(4L)).thenReturn(Optional.of(capa));
        when(capaArchivoRepository.save(any(CapaArchivo.class))).thenAnswer(invocation -> {
            CapaArchivo ca = invocation.getArgument(0);
            ca.setId(7L);
            return ca;
        });

        var dto = service.adjuntarArchivo(4L, archivo, "visible.txt", 20L);

        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getNombreVisible()).isEqualTo("visible.txt");
        assertThat(dto.getContentType()).isEqualTo("text/plain");

        Path rutaArchivo = tempDir.resolve(Path.of("uploads", "capas", dto.getNombreArchivo()));
        assertThat(Files.exists(rutaArchivo)).isTrue();
    }

    @Test
    void descargarArchivoRetornaContenidoYNombreVisible() throws Exception {
        System.setProperty("user.dir", tempDir.toString());
        Files.createDirectories(tempDir.resolve(Path.of("uploads", "capas")));
        Files.writeString(tempDir.resolve(Path.of("uploads", "capas", "almacenado.txt")), "contenido-archivo");

        CapaArchivo archivo = CapaArchivo.builder()
                .id(12L)
                .capa(Capa.builder().id(6L).build())
                .nombreArchivo("almacenado.txt")
                .nombreVisible("visible.txt")
                .contentType("text/plain")
                .tamanoBytes(20L)
                .build();

        when(capaRepository.existsById(6L)).thenReturn(true);
        when(capaArchivoRepository.findByIdAndCapa_Id(12L, 6L)).thenReturn(Optional.of(archivo));

        CapaArchivoDescargaDTO dto = service.descargarArchivo(6L, 12L);

        assertThat(dto.getNombreArchivo()).isEqualTo("visible.txt");
        assertThat(dto.getContentType()).isEqualTo("text/plain");
        assertThat(new String(dto.getContenido())).isEqualTo("contenido-archivo");
    }

    @Test
    void descargarArchivoInexistenteEnDiscoLanza404() {
        System.setProperty("user.dir", tempDir.toString());

        CapaArchivo archivo = CapaArchivo.builder()
                .id(8L)
                .capa(Capa.builder().id(2L).build())
                .nombreArchivo("no_existe.txt")
                .build();

        when(capaRepository.existsById(2L)).thenReturn(true);
        when(capaArchivoRepository.findByIdAndCapa_Id(8L, 2L)).thenReturn(Optional.of(archivo));

        assertThatThrownBy(() -> service.descargarArchivo(2L, 8L))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessageContaining("no existe")
                .satisfies(ex -> assertThat(((CustomBusinessException) ex).getCode())
                        .isEqualTo(ApiErrorCode.RECURSO_NO_ENCONTRADO));
    }
}
