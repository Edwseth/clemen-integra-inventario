package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.DocumentoFormulaMetadataDTO;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaResponseDTO;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.DocumentoFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.model.enums.TipoDocumento;
import com.willyes.clemenintegra.bom.repository.DocumentoFormulaRepository;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoFormulaServiceImplTest {

    @Mock
    private DocumentoFormulaRepository documentoRepository;
    @Mock
    private FormulaProductoRepository formulaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private BomMapper bomMapper;

    @InjectMocks
    private DocumentoFormulaServiceImpl service;

    @TempDir
    Path tempDir;

    private String originalUserDir;

    @BeforeEach
    void setUp() {
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.dir", originalUserDir);
    }

    @Test
    @DisplayName("guardarDocumento almacena el archivo y retorna DTO cuando la fórmula está editable")
    void guardarDocumento_formulaEditable() throws Exception {
        FormulaProducto formula = new FormulaProducto();
        formula.setId(1L);
        formula.setEstado(EstadoFormula.BORRADOR);

        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombreCompleto("Usuario Test");

        when(formulaRepository.findById(1L)).thenReturn(Optional.of(formula));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(usuario));
        when(documentoRepository.save(any(DocumentoFormula.class))).thenAnswer(invocation -> {
            DocumentoFormula doc = invocation.getArgument(0);
            doc.setId(15L);
            return doc;
        });
        when(bomMapper.toResponseDTO(any(DocumentoFormula.class))).thenAnswer(invocation -> {
            DocumentoFormula doc = invocation.getArgument(0);
            return DocumentoFormulaResponseDTO.builder()
                    .id(doc.getId())
                    .formulaId(doc.getFormula() != null ? doc.getFormula().getId() : null)
                    .tipoDocumento(doc.getTipoDocumento() != null ? doc.getTipoDocumento().name() : null)
                    .nombreArchivo(doc.getRutaArchivo() != null
                            ? java.nio.file.Paths.get(doc.getRutaArchivo()).getFileName().toString()
                            : null)
                    .nombreVisible(doc.getNombreArchivo())
                    .rutaArchivo(doc.getRutaArchivo())
                    .fechaRegistro(doc.getFechaSubida())
                    .usuarioCreador(doc.getUsuario() != null ? doc.getUsuario().getNombreCompleto() : null)
                    .build();
        });

        MultipartFile archivo = new MockMultipartFile(
                "archivo",
                "procedimiento.pdf",
                "application/pdf",
                "contenido".getBytes());

        DocumentoFormulaMetadataDTO metadata = DocumentoFormulaMetadataDTO.builder()
                .tipoDocumento("PROCEDIMIENTO")
                .nombreVisible("Procedimiento de prueba")
                .build();

        DocumentoFormulaResponseDTO respuesta = service.guardarDocumento(1L, archivo, metadata, 99L);

        ArgumentCaptor<DocumentoFormula> captor = ArgumentCaptor.forClass(DocumentoFormula.class);
        verify(documentoRepository).save(captor.capture());
        DocumentoFormula almacenado = captor.getValue();

        assertThat(almacenado.getFormula()).isEqualTo(formula);
        assertThat(almacenado.getUsuario()).isEqualTo(usuario);
        assertThat(almacenado.getTipoDocumento()).isEqualTo(TipoDocumento.PROCEDIMIENTO);
        assertThat(almacenado.getNombreArchivo()).isEqualTo("Procedimiento de prueba");
        assertThat(almacenado.getFormula()).isEqualTo(formula);
        assertThat(almacenado.getUsuario()).isEqualTo(usuario);
        assertThat(almacenado.getTipoDocumento()).isEqualTo(TipoDocumento.PROCEDIMIENTO);
        assertThat(almacenado.getNombreArchivo()).isEqualTo("Procedimiento de prueba");

        // 🔧 Normalizar la ruta para que funcione en Windows y Linux
        String rutaNormalizada = almacenado.getRutaArchivo().replace("\\", "/");
        assertThat(rutaNormalizada).startsWith("uploads/bom/formulas/1/");

        assertThat(almacenado.getFechaSubida()).isNotNull();

        assertThat(almacenado.getFechaSubida()).isNotNull();

        Path rutaFisica = tempDir.resolve(almacenado.getRutaArchivo());
        assertThat(Files.exists(rutaFisica)).isTrue();

        assertThat(respuesta.getId()).isEqualTo(15L);
        assertThat(respuesta.getFormulaId()).isEqualTo(1L);
        assertThat(respuesta.getNombreVisible()).isEqualTo("Procedimiento de prueba");
    }

    @Test
    @DisplayName("guardarDocumento lanza excepción cuando la fórmula no permite modificaciones")
    void guardarDocumento_formulaNoEditable() {
        FormulaProducto formula = new FormulaProducto();
        formula.setId(2L);
        formula.setEstado(EstadoFormula.APROBADA);

        when(formulaRepository.findById(2L)).thenReturn(Optional.of(formula));

        MultipartFile archivo = new MockMultipartFile(
                "archivo",
                "msds.pdf",
                "application/pdf",
                "contenido".getBytes());

        assertThatThrownBy(() -> service.guardarDocumento(2L, archivo, null, 10L))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.OPERACION_NO_PERMITIDA);
    }

    @Test
    @DisplayName("eliminarDocumento respeta la restricción de estado")
    void eliminarDocumento_formulaNoEditable() {
        FormulaProducto formula = new FormulaProducto();
        formula.setId(3L);
        formula.setEstado(EstadoFormula.APROBADA);

        DocumentoFormula documento = new DocumentoFormula();
        documento.setId(5L);
        documento.setFormula(formula);
        documento.setRutaArchivo("uploads/bom/formulas/3/doc.pdf");

        when(documentoRepository.findById(5L)).thenReturn(Optional.of(documento));
        when(formulaRepository.findById(3L)).thenReturn(Optional.of(formula));

        assertThatThrownBy(() -> service.eliminarDocumento(5L, 12L))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.OPERACION_NO_PERMITIDA)
                .hasMessageContaining("no permite agregar o eliminar documentos");
    }

    @Test
    @DisplayName("listarDocumentos devuelve los documentos asociados a la fórmula")
    void listarDocumentos_formulaExistente() {
        FormulaProducto formula = new FormulaProducto();
        formula.setId(7L);

        DocumentoFormula doc1 = new DocumentoFormula();
        doc1.setId(1L);
        doc1.setFormula(formula);
        doc1.setRutaArchivo("uploads/bom/formulas/7/doc1.pdf");
        doc1.setNombreArchivo("Doc 1");
        doc1.setFechaSubida(LocalDateTime.now());

        DocumentoFormula doc2 = new DocumentoFormula();
        doc2.setId(2L);
        doc2.setFormula(formula);
        doc2.setRutaArchivo("uploads/bom/formulas/7/doc2.pdf");
        doc2.setNombreArchivo("Doc 2");
        doc2.setFechaSubida(LocalDateTime.now());

        when(formulaRepository.findById(7L)).thenReturn(Optional.of(formula));
        when(documentoRepository.findByFormula_Id(7L)).thenReturn(List.of(doc1, doc2));

        when(bomMapper.toResponseDTO(doc1)).thenReturn(DocumentoFormulaResponseDTO.builder().id(1L).build());
        when(bomMapper.toResponseDTO(doc2)).thenReturn(DocumentoFormulaResponseDTO.builder().id(2L).build());

        List<DocumentoFormulaResponseDTO> resultado = service.listarDocumentos(7L);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(DocumentoFormulaResponseDTO::getId).containsExactly(1L, 2L);
    }
}
