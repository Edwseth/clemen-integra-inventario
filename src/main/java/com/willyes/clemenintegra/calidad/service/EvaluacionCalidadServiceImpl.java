package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.mapper.EvaluacionCalidadMapper;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EvaluacionCalidadServiceImpl implements EvaluacionCalidadService {

    private final EvaluacionCalidadRepository repository;
    private final LoteProductoRepository loteRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final EvaluacionCalidadMapper mapper;

    public Page<EvaluacionCalidadResponseDTO> listar(ResultadoEvaluacion resultado, Pageable pageable) {
        Page<EvaluacionCalidad> page = (resultado != null)
                ? repository.findByResultado(resultado, pageable)
                : repository.findAll(pageable);
        return page.map(mapper::toResponseDTO);
    }

    public EvaluacionCalidadResponseDTO crear(EvaluacionCalidadRequestDTO dto, MultipartFile archivo) {
        LoteProducto lote = loteRepository.findById(dto.getLoteProductoId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteProductoId()));

        Usuario user = usuarioService.obtenerUsuarioAutenticado();

        String rutaArchivo = null;
        if (archivo != null && !archivo.isEmpty()) {
            try {
                Path uploadDir = Paths.get("uploads", "evaluaciones");
                Files.createDirectories(uploadDir);
                String fileName = System.currentTimeMillis() + "_" + archivo.getOriginalFilename();
                Path filePath = uploadDir.resolve(fileName);
                archivo.transferTo(filePath.toFile());
                rutaArchivo = filePath.toString();
            } catch (IOException e) {
                throw new RuntimeException("Error al guardar el archivo adjunto", e);
            }
        }

        EvaluacionCalidad entidad = mapper.toEntity(dto, lote, user);
        entidad.setFechaEvaluacion(LocalDateTime.now());
        entidad.setArchivoAdjunto(rutaArchivo);

        // 👉 Lógica para liberar el lote si es aprobado
        if (dto.getResultado() == ResultadoEvaluacion.APROBADO && lote.getEstado() == EstadoLote.EN_CUARENTENA) {
            lote.setEstado(EstadoLote.DISPONIBLE);
            lote.setFechaLiberacion(LocalDate.now());
            loteRepository.save(lote);
        }

        return mapper.toResponseDTO(repository.save(entidad));
    }

    public EvaluacionCalidadResponseDTO actualizar(Long id, EvaluacionCalidadRequestDTO dto) {
        EvaluacionCalidad existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada con ID: " + id));

        LoteProducto lote = loteRepository.findById(dto.getLoteProductoId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteProductoId()));

        Usuario user = usuarioRepository.findById(dto.getUsuarioEvaluadorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getUsuarioEvaluadorId()));

        existing.setResultado(dto.getResultado());
        existing.setObservaciones(dto.getObservaciones());
        existing.setArchivoAdjunto(dto.getArchivoAdjunto());
        existing.setLoteProducto(lote);
        existing.setUsuarioEvaluador(user);
        existing.setFechaEvaluacion(LocalDateTime.now());

        // 👉 Lógica adicional: liberar lote si es aprobado
        if (dto.getResultado() == ResultadoEvaluacion.APROBADO && lote.getEstado() == EstadoLote.EN_CUARENTENA) {
            lote.setEstado(EstadoLote.DISPONIBLE);
            lote.setFechaLiberacion(LocalDate.now());
            loteRepository.save(lote);
        }

        return mapper.toResponseDTO(repository.save(existing));
    }

    public EvaluacionCalidadResponseDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada con ID: " + id));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
