package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadCreateRequest;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadVersionDownloadDTO;
import com.willyes.clemenintegra.calidad.model.DocumentoCalidad;
import com.willyes.clemenintegra.calidad.model.DocumentoCalidadVersion;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadEstado;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import com.willyes.clemenintegra.calidad.repository.DocumentoCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.DocumentoCalidadVersionRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoCalidadServiceImplTest {

    @Mock
    private DocumentoCalidadRepository documentoRepository;

    @Mock
    private DocumentoCalidadVersionRepository versionRepository;

    @Mock
    private LoteProductoRepository loteProductoRepository;

    @InjectMocks
    private DocumentoCalidadServiceImpl service;

    @TempDir
    Path tempDir;

    @Test
    void creaSubeYDescargaVersion() throws Exception {
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        DocumentoCalidadCreateRequest request = new DocumentoCalidadCreateRequest();
        request.setTipo(DocumentoCalidadTipo.SANITIZACION);
        request.setNombre("Procedimiento limpieza");

        when(documentoRepository.save(any(DocumentoCalidad.class))).thenAnswer(invocation -> {
            DocumentoCalidad doc = invocation.getArgument(0);
            doc.setId(1L);
            doc.setFechaCreacion(LocalDateTime.now());
            doc.setEstado(DocumentoCalidadEstado.VIGENTE);
            return doc;
        });

        service.crear(request, 10L);

        DocumentoCalidad documento = DocumentoCalidad.builder()
                .id(1L)
                .tipo(DocumentoCalidadTipo.SANITIZACION)
                .estado(DocumentoCalidadEstado.VIGENTE)
                .nombre("Procedimiento limpieza")
                .build();

        when(documentoRepository.findById(1L)).thenReturn(Optional.of(documento));
        when(versionRepository.findTopByDocumento_IdOrderByVersionDesc(1L)).thenReturn(Optional.empty());
        when(versionRepository.save(any(DocumentoCalidadVersion.class))).thenAnswer(invocation -> {
            DocumentoCalidadVersion version = invocation.getArgument(0);
            version.setId(5L);
            return version;
        });

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "manual.pdf",
                "application/pdf",
                "contenido".getBytes()
        );

        var versionDto = service.subirVersion(1L, archivo, "Manual", 10L);
        assertThat(versionDto.getId()).isEqualTo(5L);
        assertThat(versionDto.getVersion()).isEqualTo(1);

        Path almacenado = tempDir.resolve(versionDto.getNombreArchivo());
        assertThat(Files.exists(almacenado)).isTrue();

        DocumentoCalidadVersion versionEntity = DocumentoCalidadVersion.builder()
                .id(5L)
                .documento(documento)
                .version(1)
                .nombreArchivo(versionDto.getNombreArchivo())
                .nombreVisible("Manual")
                .contentType("application/pdf")
                .storagePath(almacenado.toString())
                .build();

        when(versionRepository.findById(anyLong())).thenReturn(Optional.of(versionEntity));

        DocumentoCalidadVersionDownloadDTO descarga = service.descargarVersion(5L);
        assertThat(descarga.getContenido()).isNotEmpty();
        assertThat(descarga.getContentType()).isEqualTo("application/pdf");
    }
}
