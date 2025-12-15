package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudMovimientoServiceOrdenProduccionFiltroTest {

    @Mock
    private SolicitudMovimientoRepository repository;
    @Mock
    private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteProductoRepository loteRepository;
    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private ReservaLoteService reservaLoteService;
    @Mock
    private LoteCalidadValidator loteCalidadValidator;

    @InjectMocks
    private SolicitudMovimientoServiceImpl service;

    @BeforeEach
    void setup() {
        lenient().when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
    }

    @Test
    void listarSolicitudes_aplicaFiltroPorOrdenProduccion() {
        Page<?> resultado = service.listarSolicitudes(null, null, null, null, 15L, null, null, Pageable.unpaged());

        ArgumentCaptor<Specification<SolicitudMovimiento>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(captor.capture(), eq(Pageable.unpaged()));

        Specification<SolicitudMovimiento> specification = captor.getValue();

        Root<SolicitudMovimiento> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        Join<Object, Object> join = mock(Join.class);
        @SuppressWarnings("unchecked")
        Path<Object> path = mock(Path.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);

        when(root.join("ordenProduccion", JoinType.LEFT)).thenReturn(join);
        when(join.get("id")).thenReturn(path);
        when(cb.equal(path, 15L)).thenReturn(predicate);

        specification.toPredicate(root, query, cb);

        verify(cb).equal(path, 15L);
        verify(root).join("ordenProduccion", JoinType.LEFT);
        verify(join).get("id");
        verify(cb).equal(path, 15L);
    }
}
