package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCapa;
import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.dto.NoConformidadDTO;
import com.willyes.clemenintegra.calidad.mapper.NoConformidadMapper;
import com.willyes.clemenintegra.calidad.repository.CapaRepository;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.calidad.repository.RetencionLoteRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class NoConformidadServiceImplTest {

    @Mock
    private NoConformidadRepository repository;
    @Mock
    private RetencionLoteRepository retencionLoteRepository;
    @Mock
    private RetencionLoteService retencionLoteService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CapaRepository capaRepository;
    @Mock
    private NoConformidadMapper mapper;

    @InjectMocks
    private NoConformidadServiceImpl service;

    private NoConformidad abierta;

    @BeforeEach
    void setup() {
        abierta = NoConformidad.builder()
                .id(1L)
                .estado(EstadoNoConformidad.ABIERTA)
                .actualizadoPor(10L)
                .actualizadoEn(LocalDateTime.now())
                .build();
    }

    @Test
    void noPermiteCerrarSiNoExisteCapaCerrada() {
        when(repository.findById(1L)).thenReturn(Optional.of(abierta));
        when(capaRepository.existsByNoConformidad_IdAndEstado(1L, EstadoCapa.CERRADA)).thenReturn(false);

        assertThatThrownBy(() -> service.cerrar(1L, new Usuario()))
                .isInstanceOf(CustomBusinessException.class)
                .satisfies(ex -> assertThat(((CustomBusinessException) ex).getCode()).isEqualTo(ApiErrorCode.NC_CAPA_REQUERIDA));
    }

    @Test
    void cierraCorrectamenteCuandoExisteCapaCerrada() {
        when(repository.findById(1L)).thenReturn(Optional.of(abierta));
        when(capaRepository.existsByNoConformidad_IdAndEstado(1L, EstadoCapa.CERRADA)).thenReturn(true);
        when(repository.save(any(NoConformidad.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDTO(any(NoConformidad.class))).thenReturn(NoConformidadDTO.builder().estado(EstadoNoConformidad.CERRADA).build());

        service.cerrar(1L, new Usuario());

        ArgumentCaptor<NoConformidad> captor = ArgumentCaptor.forClass(NoConformidad.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoNoConformidad.CERRADA);
        assertThat(captor.getValue().getFechaCierre()).isNotNull();
    }
}
