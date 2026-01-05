package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;
import com.willyes.clemenintegra.calidad.mapper.RetencionLoteMapper;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.calidad.repository.RetencionLoteRepository;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.BitacoraCambiosInventarioService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
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
    @Mock
    private NoConformidadRepository noConformidadRepository;
    @Mock
    private BitacoraCambiosInventarioService bitacoraCambiosInventarioService;

    @InjectMocks
    private RetencionLoteServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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
        usuario.setNombreCompleto("Jefe Calidad");
        usuario.setNombreUsuario("jcalidad");

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
        verify(bitacoraCambiosInventarioService).crear(any());
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

    @Test
    void levantarRetencionSinObservacionFalla() {
        autenticarComoJefe();
        assertThatThrownBy(() -> service.levantarRetencion(10L, null, " "))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.OBSERVACION_REQUERIDA);
    }

    @Test
    void levantarRetencionInexistenteDevuelveCodigoDominio() {
        autenticarComoJefe();

        when(repository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.levantarRetencion(77L, null, "obs"))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.RETENCION_NO_ENCONTRADA);
    }

    @Test
    void levantarRetencionNoActivaBloqueaOperacion() {
        autenticarComoJefe();

        RetencionLote retencion = RetencionLote.builder()
                .id(88L)
                .estado(EstadoRetencion.LIBERADO)
                .build();

        when(repository.findById(88L)).thenReturn(Optional.of(retencion));

        assertThatThrownBy(() -> service.levantarRetencion(88L, null, "obs"))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.RETENCION_NO_ACTIVA);
    }

    @Test
    void noPermiteLevantarRetencionConNcAbierta() {
        autenticarComoJefe();

        LoteProducto lote = new LoteProducto();
        lote.setId(9L);
        lote.setEstado(EstadoLote.RETENIDO);

        RetencionLote retencion = RetencionLote.builder()
                .id(22L)
                .estado(EstadoRetencion.RETENIDO)
                .motivo(MotivoRetencion.NO_CONFORMIDAD)
                .lote(lote)
                .noConformidad(NoConformidad.builder().id(15L).build())
                .build();

        NoConformidad nc = NoConformidad.builder()
                .id(15L)
                .estado(EstadoNoConformidad.ABIERTA)
                .build();

        when(repository.findById(22L)).thenReturn(Optional.of(retencion));
        when(noConformidadRepository.findById(15L)).thenReturn(Optional.of(nc));
        when(noConformidadRepository.findByLote_Id(9L)).thenReturn(List.of(nc));

        assertThatThrownBy(() -> service.levantarRetencion(22L, null, "obs"))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.RETENCION_NC_NO_CERRADA);
        assertThat(retencion.getEstado()).isEqualTo(EstadoRetencion.RETENIDO);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.RETENIDO);
    }

    @Test
    void levantarRetencionConNcCerradaActualizaLoteACuarentena() {
        autenticarComoJefe();

        LoteProducto lote = new LoteProducto();
        lote.setId(30L);
        lote.setEstado(EstadoLote.RETENIDO);

        RetencionLote retencion = RetencionLote.builder()
                .id(40L)
                .estado(EstadoRetencion.RETENIDO)
                .motivo(MotivoRetencion.NO_CONFORMIDAD)
                .lote(lote)
                .noConformidad(NoConformidad.builder().id(55L).build())
                .build();

        NoConformidad nc = NoConformidad.builder()
                .id(55L)
                .estado(EstadoNoConformidad.CERRADA)
                .build();

        when(repository.findById(40L)).thenReturn(Optional.of(retencion));
        when(noConformidadRepository.findById(55L)).thenReturn(Optional.of(nc));
        when(noConformidadRepository.findByLote_Id(30L)).thenReturn(List.of(nc));
        when(repository.save(any(RetencionLote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByLote_IdAndEstado(30L, EstadoRetencion.RETENIDO)).thenReturn(List.of());
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(null);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioAprobador()));

        RetencionLote resultado = service.levantarRetencion(40L, usuarioAprobador(), "obs");

        assertThat(resultado.getEstado()).isEqualTo(EstadoRetencion.LIBERADO);
        assertThat(resultado.getFechaLiberacion()).isNotNull();
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.EN_CUARENTENA);
        verify(bitacoraCambiosInventarioService).crear(any());
    }

    @Test
    void levantarRetencionSinNcAsociadaPermiteCambio() {
        autenticarComoJefe();

        LoteProducto lote = new LoteProducto();
        lote.setId(60L);
        lote.setEstado(EstadoLote.RETENIDO);

        RetencionLote retencion = RetencionLote.builder()
                .id(70L)
                .estado(EstadoRetencion.RETENIDO)
                .motivo(MotivoRetencion.NO_CONFORMIDAD)
                .lote(lote)
                .build();

        when(repository.findById(70L)).thenReturn(Optional.of(retencion));
        when(noConformidadRepository.findByLote_Id(60L)).thenReturn(List.of());
        when(repository.save(any(RetencionLote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByLote_IdAndEstado(60L, EstadoRetencion.RETENIDO)).thenReturn(List.of());
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(null);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioAprobador()));

        RetencionLote resultado = service.levantarRetencion(70L, usuarioAprobador(), "obs");

        assertThat(resultado.getEstado()).isEqualTo(EstadoRetencion.LIBERADO);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.EN_CUARENTENA);
        verify(bitacoraCambiosInventarioService, org.mockito.Mockito.atLeastOnce()).crear(any());
    }

    private void autenticarComoJefe() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "jefe", "pass", List.of(new SimpleGrantedAuthority("ROL_JEFE_CALIDAD")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Usuario usuarioAprobador() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombreCompleto("Jefe Calidad");
        usuario.setNombreUsuario("jcalidad");
        return usuario;
    }
}
