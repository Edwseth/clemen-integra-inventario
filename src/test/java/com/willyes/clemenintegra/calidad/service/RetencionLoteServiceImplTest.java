package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;
import com.willyes.clemenintegra.calidad.mapper.RetencionLoteMapper;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.repository.RetencionLoteRepository;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetencionLoteServiceImplTest {

    @Mock
    private RetencionLoteRepository repository;
    @Mock
    private LoteProductoRepository loteRepository;
    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private InventoryCatalogResolver catalogResolver;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RetencionLoteMapper mapper;

    @InjectMocks
    private RetencionLoteServiceImpl service;

    @Test
    void crearRetencionManualActualizaLoteYGuardaRetencion() {
        RetencionLoteDTO dto = RetencionLoteDTO.builder()
                .loteId(1L)
                .causa("Observación de retención")
                .motivo(MotivoRetencion.OTRO)
                .fechaLiberacion(LocalDateTime.now().plusDays(2))
                .aprobadoPorId(5L)
                .build();

        LoteProducto lote = new LoteProducto();
        lote.setId(1L);
        lote.setEstado(EstadoLote.LIBERADO);
        lote.setAlmacen(new Almacen(10));

        Usuario usuario = new Usuario();
        usuario.setId(5L);

        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(99L);
        when(almacenRepository.findById(99L)).thenReturn(Optional.of(new Almacen(99)));
        when(repository.save(any(RetencionLote.class))).thenAnswer(invocation -> {
            RetencionLote retencion = invocation.getArgument(0);
            retencion.setId(55L);
            return retencion;
        });
        when(mapper.toDTO(any(RetencionLote.class))).thenAnswer(invocation -> {
            RetencionLote retencion = invocation.getArgument(0);
            return RetencionLoteDTO.builder()
                    .id(retencion.getId())
                    .estado(retencion.getEstado())
                    .build();
        });

        RetencionLoteDTO respuesta = service.crear(dto);

        ArgumentCaptor<RetencionLote> captor = ArgumentCaptor.forClass(RetencionLote.class);
        assertThat(respuesta.getId()).isEqualTo(55L);
        assertThat(respuesta.getEstado()).isEqualTo(EstadoRetencion.RETENIDO);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.RETENIDO);
        assertThat(lote.getAlmacen()).isNotNull();
        assertThat(lote.getAlmacen().getId()).isEqualTo(99);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoRetencion.RETENIDO);
        assertThat(captor.getValue().getFechaRetencion()).isNotNull();
    }

    @Test
    void rechazaMotivoInvalidoEnCrear() {
        RetencionLoteDTO dto = RetencionLoteDTO.builder()
                .loteId(2L)
                .causa("Causa")
                .aprobadoPorId(3L)
                .build();

        assertThatThrownBy(() -> service.crear(dto))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.RETENCION_MOTIVO_INVALIDO);
    }
}
