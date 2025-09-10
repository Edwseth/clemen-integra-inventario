package com.willyes.clemenintegra.inventario;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.LoteVencimientoScheduler;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class LoteVencimientoSchedulerTest {

    @Autowired
    private LoteProductoRepository loteProductoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private AlmacenRepository almacenRepository;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private LoteVencimientoScheduler scheduler;

    @BeforeEach
    void setup() {
        loteProductoRepository.deleteAll();
        productoRepository.deleteAll();
        almacenRepository.deleteAll();
        categoriaProductoRepository.deleteAll();
        unidadMedidaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void lotesVencidosSeMarcanComoVencidos() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("user")
                .clave("pwd")
                .nombreCompleto("User Test")
                .correo("user@test.com")
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .activo(true)
                .bloqueado(false)
                .build());

        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Litro")
                .simbolo("L")
                .build());

        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Cat")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("SKU1")
                .nombre("Prod1")
                .descripcionProducto("desc")
                .stockMinimo(BigDecimal.ZERO)
                .tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidad)
                .categoriaProducto(categoria)
                .creadoPor(usuario)
                .build());

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Alm1")
                .ubicacion("Loc")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        LoteProducto vencido = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("L1")
                .fechaFabricacion(LocalDateTime.now().minusDays(10))
                .fechaVencimiento(LocalDateTime.now().minusDays(1))
                .stockLote(BigDecimal.ONE)
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacen)
                .build());

        LoteProducto vigente = loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("L2")
                .fechaFabricacion(LocalDateTime.now())
                .fechaVencimiento(LocalDateTime.now().plusDays(5))
                .stockLote(BigDecimal.ONE)
                .estado(EstadoLote.DISPONIBLE)
                .producto(producto)
                .almacen(almacen)
                .build());

        scheduler.actualizarLotesVencidos();

        assertEquals(EstadoLote.VENCIDO, loteProductoRepository.findById(vencido.getId()).orElseThrow().getEstado());
        assertEquals(EstadoLote.DISPONIBLE, loteProductoRepository.findById(vigente.getId()).orElseThrow().getEstado());
    }
}
