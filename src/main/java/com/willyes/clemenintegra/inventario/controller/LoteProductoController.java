package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lotes")
@RequiredArgsConstructor
public class LoteProductoController {

    private final LoteProductoService service;
    private final LoteProductoRepository loteProductoRepository;
    private final LoteProductoMapper mapper;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<LoteProductoResponseDTO> crearLote(@RequestBody LoteProductoRequestDTO dto) {
        LoteProductoResponseDTO response = service.crearLote(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<LoteProductoResponseDTO>> listarPorEstado(@PathVariable String estado) {
        List<LoteProductoResponseDTO> lotes = service.obtenerLotesPorEstado(estado);
        return ResponseEntity.ok(lotes);
    }

    @GetMapping("/reporte-vencimiento")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ANALISTA_CALIDAD', 'ROL_JEFE_CALIDAD', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarLotesPorVencer() throws IOException {
        Workbook workbook = service.generarReporteLotesPorVencerExcel();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lotes_por_vencer.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(bos.toByteArray());
    }

    @GetMapping("/reporte-alertas")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ANALISTA_CALIDAD', 'ROL_JEFE_CALIDAD', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarAlertasActivas() {
        ByteArrayOutputStream stream = service.generarReporteAlertasActivasExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alertas_activas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(stream.toByteArray());
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<LoteProductoResponseDTO>> listarTodos(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<LoteProductoResponseDTO> lotes = service.listarTodos(pageable);
        return ResponseEntity.ok(lotes);
    }

    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD', 'ROL_ANALISTA_CALIDAD', 'ROL_SUPER_ADMIN')")
    @GetMapping("/por-evaluar")
    public ResponseEntity<List<LoteProductoResponseDTO>> obtenerLotesPorEvaluar() {
        List<LoteProducto> lotes = loteProductoRepository.findByEstadoIn(List.of(EstadoLote.EN_CUARENTENA, EstadoLote.RETENIDO));
        List<LoteProductoResponseDTO> resultado = lotes.stream()
                .map(lote -> mapper.toDto(lote))
                .collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

}

