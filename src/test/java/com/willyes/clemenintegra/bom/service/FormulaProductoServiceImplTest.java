package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.FormulaProductoResponse;
import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FormulaProductoServiceImplTest {

    @Autowired
    private FormulaProductoServiceImpl service;
    @Autowired
    private FormulaProductoRepository formulaRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private LoteProductoRepository loteProductoRepository;
    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AlmacenRepository almacenRepository;

    @BeforeEach
    void setup() {
        loteProductoRepository.deleteAll();
        formulaRepository.deleteAll();
        productoRepository.deleteAll();
        unidadMedidaRepository.deleteAll();
        categoriaProductoRepository.deleteAll();
        usuarioRepository.deleteAll();
        almacenRepository.deleteAll();
    }

    @Test
    void disponibilidadExcluyeAgotadosYReservados() {
        UnidadMedida unidad = unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Kg").simbolo("kg").build());
        CategoriaProducto categoria = categoriaProductoRepository.save(CategoriaProducto.builder()
                .nombre("Mat").tipo(TipoCategoria.MATERIA_PRIMA).build());
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("user").clave("pwd").nombreCompleto("User")
                .correo("u@t.com").rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true).bloqueado(false).build());

        Producto insumo = productoRepository.save(Producto.builder()
                .codigoSku("INS1").nombre("Insumo1").descripcionProducto("d")
                .stockMinimo(BigDecimal.ZERO).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidad).categoriaProducto(categoria).creadoPor(usuario)
                .build());
        Producto producto = productoRepository.save(Producto.builder()
                .codigoSku("PROD1").nombre("Producto1").descripcionProducto("d")
                .stockMinimo(BigDecimal.ZERO).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                .unidadMedida(unidad).categoriaProducto(categoria).creadoPor(usuario)
                .build());

        DetalleFormula detalle = DetalleFormula.builder()
                .insumo(insumo)
                .unidadMedida(unidad)
                .cantidadNecesaria(BigDecimal.ONE)
                .obligatorio(true)
                .build();
        FormulaProducto formula = FormulaProducto.builder()
                .producto(producto)
                .estado(EstadoFormula.APROBADA)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .creadoPor(usuario)
                .detalles(List.of(detalle))
                .build();
        detalle.setFormula(formula);
        formulaRepository.save(formula);

        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Main").ubicacion("U")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build());

        loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("L1")
                .producto(insumo)
                .almacen(almacen)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("10"))
                .stockReservado(new BigDecimal("3"))
                .agotado(false)
                .build());

        loteProductoRepository.save(LoteProducto.builder()
                .codigoLote("L2")
                .producto(insumo)
                .almacen(almacen)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("5"))
                .stockReservado(BigDecimal.ZERO)
                .agotado(true)
                .fechaAgotado(LocalDateTime.now())
                .build());

        FormulaProductoResponse resp = service.obtenerFormulaActivaPorProducto(producto.getId().longValue(), BigDecimal.ONE);

        BigDecimal esperado = new BigDecimal("7");
        assertEquals(0, esperado.compareTo(resp.detalles.get(0).stockDisponible));
        assertEquals(1, resp.detalles.get(0).lotes.size());
        assertEquals("L1", resp.detalles.get(0).lotes.get(0).getCodigoLote());
    }
}

