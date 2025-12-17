package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.DocumentoMetaDTO;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDocumento;
import com.willyes.clemenintegra.inventario.model.enums.TipoDocumentoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDocumentoRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class OrdenCompraDocumentoServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock
    private OrdenCompraRepository ordenCompraRepository;
    @Mock
    private OrdenCompraDocumentoRepository documentoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private OrdenCompraDocumentoServiceImpl service;

    private AutoCloseable closeable;
    private String originalUserDir;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setProperty("user.dir", originalUserDir);
        closeable.close();
    }

    @Test
    void subirDocumentos_guardaArchivosYRegistros() throws IOException {
        OrdenCompra oc = new OrdenCompra();
        oc.setId(5);
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombreCompleto("Tester");

        when(ordenCompraRepository.findById(anyLong())).thenReturn(java.util.Optional.of(oc));
        when(usuarioRepository.findById(anyLong())).thenReturn(java.util.Optional.of(usuario));

        AtomicLong idGen = new AtomicLong(1);
        doAnswer(invocation -> {
            OrdenCompraDocumento doc = invocation.getArgument(0);
            doc.setId(idGen.getAndIncrement());
            doc.setFechaCreacion(LocalDateTime.now());
            return doc;
        }).when(documentoRepository).save(any(OrdenCompraDocumento.class));

        MockMultipartFile archivo1 = new MockMultipartFile("archivos", "cert.pdf", "application/pdf", "data1".getBytes());
        MockMultipartFile archivo2 = new MockMultipartFile("archivos", "factura.png", "image/png", "data2".getBytes());

        List<DocumentoMetaDTO> metadata = List.of(
                DocumentoMetaDTO.builder().nombreVisible("Certificado A").tipoDocumento(TipoDocumentoOrdenCompra.CERTIFICADO).build(),
                DocumentoMetaDTO.builder().nombreVisible("Factura 1").tipoDocumento(TipoDocumentoOrdenCompra.FACTURA).build()
        );

        var respuesta = service.subirDocumentos(5L, List.of(archivo1, archivo2), metadata, usuario.getId());

        assertThat(respuesta).hasSize(2);
        Path expectedDir = tempDir.resolve(Path.of("uploads", "ordenes-compra", "5"));
        assertThat(Files.exists(expectedDir)).isTrue();
        assertThat(Files.list(expectedDir)).hasSize(2);
    }

    @Test
    void subirDocumentos_lanzaErrorSinMetadata() {
        OrdenCompra oc = new OrdenCompra();
        oc.setId(5);
        Usuario usuario = new Usuario();
        usuario.setId(10L);

        when(ordenCompraRepository.findById(anyLong())).thenReturn(java.util.Optional.of(oc));
        when(usuarioRepository.findById(anyLong())).thenReturn(java.util.Optional.of(usuario));

        MockMultipartFile archivo = new MockMultipartFile("archivos", "cert.pdf", "application/pdf", "data1".getBytes());

        assertThatThrownBy(() -> service.subirDocumentos(5L, List.of(archivo), List.of(), usuario.getId()))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessageContaining("metadata");
    }
}
