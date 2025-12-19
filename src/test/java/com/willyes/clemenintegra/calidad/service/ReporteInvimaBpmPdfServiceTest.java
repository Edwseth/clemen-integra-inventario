package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadResumenDTO;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteInvimaBpmPdfServiceTest {

    @Mock
    private AuditoriaLoteService auditoriaLoteService;

    @Mock
    private DocumentoCalidadService documentoCalidadService;

    @Mock
    private LoteProductoRepository loteProductoRepository;

    @InjectMocks
    private ReporteInvimaBpmPdfService service;

    @Test
    void generaPdfConDocumentosYLoteValido() {
        AuditoriaLoteResponseDTO auditoria = AuditoriaLoteResponseDTO.builder()
                .loteId(1L)
                .codigoLote("LOT-01")
                .nombreProducto("Producto")
                .categoriaProducto("Categoria")
                .estadoLote("LIBERADO")
                .nombreAlmacenActual("Principal")
                .ubicacionAlmacenActual("A1")
                .evaluaciones(List.of(AuditoriaLoteResponseDTO.EvaluacionResumenDTO.builder()
                        .id(10L)
                        .resultado("CONFORME")
                        .build()))
                .build();

        when(auditoriaLoteService.obtenerAuditoriaDeLote(1L)).thenReturn(auditoria);
        when(loteProductoRepository.findById(1L)).thenReturn(Optional.of(LoteProducto.builder()
                .id(1L)
                .fechaLiberacion(LocalDateTime.now())
                .build()));
        when(documentoCalidadService.listarVigentesPorLote(1L))
                .thenReturn(List.of(DocumentoCalidadResumenDTO.builder()
                        .documentoId(3L)
                        .tipo(DocumentoCalidadTipo.SANITIZACION)
                        .nombre("Procedimiento")
                        .version(1)
                        .fechaVersion(LocalDateTime.now())
                        .build()));
        when(documentoCalidadService.listarVigentesPorTipo(DocumentoCalidadTipo.SANITIZACION))
                .thenReturn(List.of());
        when(documentoCalidadService.listarVigentesPorTipo(DocumentoCalidadTipo.CALIBRACION))
                .thenReturn(List.of());
        when(documentoCalidadService.listarVigentesPorTipo(DocumentoCalidadTipo.OTRO))
                .thenReturn(List.of());

        byte[] pdf = service.generarPdf(1L);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(100);
    }

    @Test
    void lanzaNotFoundSiNoExisteLote() {
        when(auditoriaLoteService.obtenerAuditoriaDeLote(9L))
                .thenReturn(AuditoriaLoteResponseDTO.builder().loteId(9L).build());
        when(loteProductoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generarPdf(9L))
                .isInstanceOf(ResponseStatusException.class);
    }
}
