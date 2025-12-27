package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.DetalleEtapa;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.DetalleEtapaRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetalleEtapaServiceImplTest {

    @Mock
    private DetalleEtapaRepository detalleEtapaRepository;
    @Mock
    private EtapaProduccionRepository etapaProduccionRepository;
    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private DetalleEtapaServiceImpl service;

    @Test
    void guardar_asociaReferenciasValidadas() {
        OrdenProduccion orden = OrdenProduccion.builder().id(5L).build();
        EtapaProduccion etapa = EtapaProduccion.builder().id(7L).ordenProduccion(orden).build();
        Usuario operario = new Usuario(); operario.setId(11L);

        DetalleEtapa detalle = DetalleEtapa.builder()
                .ordenProduccion(OrdenProduccion.builder().id(orden.getId()).build())
                .etapaProduccion(EtapaProduccion.builder().id(etapa.getId()).build())
                .operario(operario)
                .build();

        when(etapaProduccionRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));
        when(ordenProduccionRepository.findById(orden.getId())).thenReturn(Optional.of(orden));
        when(usuarioRepository.findById(operario.getId())).thenReturn(Optional.of(operario));
        when(detalleEtapaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DetalleEtapa guardado = service.guardar(detalle);

        assertThat(guardado.getOrdenProduccion()).isSameAs(orden);
        assertThat(guardado.getEtapaProduccion()).isSameAs(etapa);
        assertThat(guardado.getOperario()).isSameAs(operario);
    }

    @Test
    void guardar_conEtapaDeOtraOrdenLanzaErrorDeNegocio() {
        OrdenProduccion orden = OrdenProduccion.builder().id(5L).build();
        OrdenProduccion otraOrden = OrdenProduccion.builder().id(8L).build();
        EtapaProduccion etapa = EtapaProduccion.builder().id(7L).ordenProduccion(otraOrden).build();
        Usuario operario = new Usuario(); operario.setId(11L);

        DetalleEtapa detalle = DetalleEtapa.builder()
                .ordenProduccion(orden)
                .etapaProduccion(EtapaProduccion.builder().id(etapa.getId()).build())
                .operario(operario)
                .build();

        when(etapaProduccionRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));
        when(ordenProduccionRepository.findById(orden.getId())).thenReturn(Optional.of(orden));
        when(usuarioRepository.findById(operario.getId())).thenReturn(Optional.of(operario));

        assertThatThrownBy(() -> service.guardar(detalle))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.SOLICITUD_INVALIDA);
    }

    @Test
    void guardar_errorDeIntegridadSeMapeaComoSolicitudInvalida() {
        OrdenProduccion orden = OrdenProduccion.builder().id(5L).build();
        EtapaProduccion etapa = EtapaProduccion.builder().id(7L).ordenProduccion(orden).build();
        Usuario operario = new Usuario(); operario.setId(11L);

        DetalleEtapa detalle = DetalleEtapa.builder()
                .ordenProduccion(orden)
                .etapaProduccion(etapa)
                .operario(operario)
                .build();

        when(etapaProduccionRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));
        when(ordenProduccionRepository.findById(orden.getId())).thenReturn(Optional.of(orden));
        when(usuarioRepository.findById(operario.getId())).thenReturn(Optional.of(operario));
        when(detalleEtapaRepository.save(any())).thenThrow(new DataIntegrityViolationException("fk error"));

        assertThatThrownBy(() -> service.guardar(detalle))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.SOLICITUD_INVALIDA);
    }
}
