package com.willyes.clemenintegra.documental.service;

import com.willyes.clemenintegra.documental.dto.DocumentoCreateRequest;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionCreateRequest;
import com.willyes.clemenintegra.documental.model.Documento;
import com.willyes.clemenintegra.documental.model.DocumentoVersion;
import com.willyes.clemenintegra.documental.model.enums.AreaDocumento;
import com.willyes.clemenintegra.documental.model.enums.EstadoDocumento;
import com.willyes.clemenintegra.documental.model.enums.TipoDocumento;
import com.willyes.clemenintegra.documental.repository.DocumentoRepository;
import com.willyes.clemenintegra.documental.repository.DocumentoVersionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlDocumentalServiceTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private DocumentoVersionRepository versionRepository;

    @InjectMocks
    private ControlDocumentalServiceImpl service;

    @TempDir
    Path tempDir;

    @Test
    void creaDocumentoYVersiones() {
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        DocumentoCreateRequest request = new DocumentoCreateRequest();
        request.setCodigo("DOC-001");
        request.setNombre("Procedimiento limpieza");
        request.setTipo(TipoDocumento.PROCEDIMIENTO);
        request.setArea(AreaDocumento.CALIDAD);

        Usuario usuario = Usuario.builder()
                .id(10L)
                .nombreUsuario("jperez")
                .nombreCompleto("Juan Perez")
                .build();

        when(documentoRepository.existsByCodigoIgnoreCase("DOC-001")).thenReturn(false);
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> {
            Documento doc = invocation.getArgument(0);
            doc.setId(1L);
            doc.setFechaCreacion(LocalDateTime.now());
            return doc;
        });

        var creado = service.crearDocumento(request, usuario);
        assertThat(creado.getEstado()).isEqualTo(EstadoDocumento.EN_ELABORACION);

        Documento documento = Documento.builder()
                .id(1L)
                .codigo("DOC-001")
                .nombre("Procedimiento limpieza")
                .tipo(TipoDocumento.PROCEDIMIENTO)
                .area(AreaDocumento.CALIDAD)
                .estado(EstadoDocumento.EN_ELABORACION)
                .creadoPor(usuario)
                .build();

        when(documentoRepository.findById(1L)).thenReturn(Optional.of(documento));
        when(versionRepository.findMaxNumeroVersionByDocumentoId(1L)).thenReturn(Optional.empty());
        when(versionRepository.findByDocumentoIdOrderByNumeroVersionDesc(1L)).thenReturn(List.of());
        when(versionRepository.save(any(DocumentoVersion.class))).thenAnswer(invocation -> {
            DocumentoVersion version = invocation.getArgument(0);
            version.setId(5L);
            return version;
        });
        when(documentoRepository.save(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "manual.pdf",
                "application/pdf",
                "contenido".getBytes()
        );

        DocumentoVersionCreateRequest versionRequest = new DocumentoVersionCreateRequest();
        versionRequest.setNombreVisible("Manual");

        var versionUno = service.agregarVersion(1L, versionRequest, archivo, usuario);
        assertThat(versionUno.getNumeroVersion()).isEqualTo(1);
        assertThat(versionUno.isVigente()).isTrue();
        assertThat(documento.getEstado()).isEqualTo(EstadoDocumento.VIGENTE);

        DocumentoVersion existente = DocumentoVersion.builder()
                .id(5L)
                .documento(documento)
                .numeroVersion(1)
                .vigente(true)
                .build();

        when(versionRepository.findMaxNumeroVersionByDocumentoId(1L)).thenReturn(Optional.of(1));
        when(versionRepository.findByDocumentoIdOrderByNumeroVersionDesc(1L)).thenReturn(List.of(existente));

        var versionDos = service.agregarVersion(1L, versionRequest, archivo, usuario);
        assertThat(versionDos.getNumeroVersion()).isEqualTo(2);
        assertThat(existente.isVigente()).isFalse();
        assertThat(versionDos.isVigente()).isTrue();
    }

    @Test
    void detalleDocumentoIncluyeVersionVigente() {
        Usuario usuario = Usuario.builder()
                .id(10L)
                .nombreUsuario("jperez")
                .nombreCompleto("Juan Perez")
                .build();

        Documento documento = Documento.builder()
                .id(1L)
                .codigo("DOC-001")
                .nombre("Procedimiento limpieza")
                .tipo(TipoDocumento.PROCEDIMIENTO)
                .area(AreaDocumento.CALIDAD)
                .estado(EstadoDocumento.VIGENTE)
                .creadoPor(usuario)
                .build();

        DocumentoVersion version = DocumentoVersion.builder()
                .id(5L)
                .documento(documento)
                .numeroVersion(2)
                .fechaEmision(LocalDateTime.now())
                .vigente(true)
                .build();

        when(documentoRepository.findById(1L)).thenReturn(Optional.of(documento));
        when(versionRepository.findTopByDocumentoIdOrderByNumeroVersionDesc(1L))
                .thenReturn(Optional.of(version));
        when(versionRepository.findByDocumentoIdOrderByNumeroVersionDesc(1L)).thenReturn(List.of(version));

        var detalle = service.obtenerDetalleDocumento(1L);

        assertThat(detalle.getDocumento().getNumeroVersionVigente()).isEqualTo(2);
        assertThat(detalle.getDocumento().getVersionVigenteId()).isEqualTo(5L);
    }
}
