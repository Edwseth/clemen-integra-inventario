package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MotivoMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MotivoMovimientoMapper;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MotivoMovimientoServiceImplTest {

    @Mock
    private MotivoMovimientoRepository repository;

    @Mock
    private MotivoMovimientoMapper mapper;

    @InjectMocks
    private MotivoMovimientoServiceImpl service;

    @Test
    @DisplayName("listar no falla cuando existe motivo CORRECCION_SALIDA_CLIENTE")
    void listar_conCorreccionSalidaCliente() {
        MotivoMovimiento motivo = MotivoMovimiento.builder()
                .id(50L)
                .descripcion("Corrección salida cliente")
                .motivo(ClasificacionMovimientoInventario.CORRECCION_SALIDA_CLIENTE)
                .build();
        MotivoMovimientoResponseDTO dto = new MotivoMovimientoResponseDTO(50L, "CORRECCION_SALIDA_CLIENTE");

        when(repository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(motivo)));
        when(mapper.toDTO(motivo)).thenReturn(dto);

        Page<MotivoMovimientoResponseDTO> result = service.listar(PageRequest.of(0, 10));

        assertThat(result.getContent()).containsExactly(dto);
    }
}
