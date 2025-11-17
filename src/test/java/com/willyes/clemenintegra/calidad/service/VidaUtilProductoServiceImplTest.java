package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.VidaUtilProductoDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.VidaUtilProductoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VidaUtilProductoServiceImplTest {

    @Mock
    private VidaUtilProductoRepository vidaUtilProductoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private VidaUtilProductoServiceImpl service;

    private Producto productoTerminado;

    @BeforeEach
    void setUp() {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);

        productoTerminado = new Producto();
        productoTerminado.setId(10);
        productoTerminado.setCodigoSku("PT-001");
        productoTerminado.setNombre("Producto Terminado");
        productoTerminado.setCategoriaProducto(categoria);
    }

    @Test
    void guardar_deberiaCrearNuevoCuandoProductoTerminadoValido() {
        VidaUtilProducto entidadGuardada = VidaUtilProducto.builder()
                .productoId(productoTerminado.getId())
                .producto(productoTerminado)
                .semanasVigencia(12)
                .build();

        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoTerminado));
        when(vidaUtilProductoRepository.findById(productoTerminado.getId())).thenReturn(Optional.empty());
        when(vidaUtilProductoRepository.save(any(VidaUtilProducto.class))).thenReturn(entidadGuardada);

        VidaUtilProducto resultado = service.guardar(productoTerminado.getId(), 12);

        assertThat(resultado.getSemanasVigencia()).isEqualTo(12);
        assertThat(resultado.getProducto()).isEqualTo(productoTerminado);
        assertThat(resultado.getProductoId()).isEqualTo(productoTerminado.getId());

        ArgumentCaptor<VidaUtilProducto> captor = ArgumentCaptor.forClass(VidaUtilProducto.class);
        verify(vidaUtilProductoRepository).save(captor.capture());
        VidaUtilProducto enviado = captor.getValue();
        assertThat(enviado.getProductoId()).isEqualTo(productoTerminado.getId());
    }

    @Test
    void guardar_deberiaActualizarCuandoExisteRegistro() {
        VidaUtilProducto existente = VidaUtilProducto.builder()
                .productoId(productoTerminado.getId())
                .producto(productoTerminado)
                .semanasVigencia(8)
                .build();

        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoTerminado));
        when(vidaUtilProductoRepository.findById(productoTerminado.getId())).thenReturn(Optional.of(existente));
        when(vidaUtilProductoRepository.save(any(VidaUtilProducto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VidaUtilProducto resultado = service.guardar(productoTerminado.getId(), 20);

        assertThat(resultado.getSemanasVigencia()).isEqualTo(20);

        ArgumentCaptor<VidaUtilProducto> captor = ArgumentCaptor.forClass(VidaUtilProducto.class);
        verify(vidaUtilProductoRepository).save(captor.capture());
        VidaUtilProducto enviado = captor.getValue();
        assertThat(enviado.getProductoId()).isEqualTo(productoTerminado.getId());
        assertThat(enviado.getSemanasVigencia()).isEqualTo(20);
    }

    @Test
    void guardar_deberiaFallarCuandoProductoNoExiste() {
        when(productoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.guardar(10, 5))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.RECURSO_NO_ENCONTRADO);
    }

    @Test
    void guardar_deberiaFallarCuandoProductoNoEsTerminado() {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.MATERIA_PRIMA);
        Producto mp = new Producto();
        mp.setId(11);
        mp.setCategoriaProducto(categoria);

        when(productoRepository.findById(11L)).thenReturn(Optional.of(mp));

        assertThatThrownBy(() -> service.guardar(11, 6))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.VIDA_UTIL_SOLO_PRODUCTO_TERMINADO);
    }

    @Test
    void guardar_deberiaFallarCuandoSemanasInvalidas() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoTerminado));

        assertThatThrownBy(() -> service.guardar(10, 0))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.VIDA_UTIL_SEMANAS_INVALIDAS);
    }

    @Test
    void listarProductosTerminados_deberiaIncluirVidaUtil() {
        VidaUtilProducto vidaUtil = VidaUtilProducto.builder()
                .productoId(productoTerminado.getId())
                .producto(productoTerminado)
                .semanasVigencia(15)
                .build();

        when(productoRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(productoTerminado)));
        when(vidaUtilProductoRepository.findAllById(List.of(productoTerminado.getId())))
                .thenReturn(List.of(vidaUtil));

        Page<VidaUtilProductoDTO> page = service.listarProductosTerminados(null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        VidaUtilProductoDTO dto = page.getContent().get(0);
        assertThat(dto.getProductoId()).isEqualTo(productoTerminado.getId());
        assertThat(dto.getSemanasVigencia()).isEqualTo(15);
    }
}
