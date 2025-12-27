package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.DetalleEtapaRequest;
import com.willyes.clemenintegra.produccion.dto.DetalleEtapaResponse;
import com.willyes.clemenintegra.produccion.model.DetalleEtapa;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.service.DetalleEtapaService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetalleEtapaControllerTest {

    @Mock
    private DetalleEtapaService detalleEtapaService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private DetalleEtapaController controller;

    @Test
    void crear_asignaFechaInicioYOperarioDelBackend() {
        DetalleEtapaRequest request = new DetalleEtapaRequest();
        request.etapaProduccionId = 7L;
        request.ordenProduccionId = 11L;
        request.fechaInicio = null;
        request.operarioId = null;

        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombreCompleto("Operario Backend");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(detalleEtapaService.guardar(any())).thenAnswer(invocation -> {
            DetalleEtapa detalle = invocation.getArgument(0);
            detalle.setId(1L);
            if (detalle.getOrdenProduccion() == null) {
                detalle.setOrdenProduccion(OrdenProduccion.builder().id(request.ordenProduccionId).build());
            }
            if (detalle.getEtapaProduccion() == null) {
                detalle.setEtapaProduccion(EtapaProduccion.builder().id(request.etapaProduccionId).build());
            }
            return detalle;
        });

        ResponseEntity<DetalleEtapaResponse> response = controller.crear(request);

        ArgumentCaptor<DetalleEtapa> detalleCaptor = ArgumentCaptor.forClass(DetalleEtapa.class);
        verify(detalleEtapaService).guardar(detalleCaptor.capture());
        DetalleEtapa enviado = detalleCaptor.getValue();

        assertThat(enviado.getFechaInicio()).isNotNull();
        assertThat(enviado.getFechaInicio()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(enviado.getOperario()).isNotNull();
        assertThat(enviado.getOperario().getId()).isEqualTo(usuario.getId());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id).isEqualTo(1L);
    }
}
