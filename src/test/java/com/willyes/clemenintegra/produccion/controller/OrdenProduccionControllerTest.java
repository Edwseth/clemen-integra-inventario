package com.willyes.clemenintegra.produccion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
public class OrdenProduccionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AlmacenRepository almacenRepository;
    @Autowired private LoteProductoRepository loteProductoRepository;
    @Autowired private OrdenProduccionRepository ordenProduccionRepository;
    @Autowired private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired private CategoriaProductoRepository categoriaProductoRepository;

    /*@BeforeEach
    void setUp() {
        // Unidad de medida
        UnidadMedida unidad = unidadMedidaRepository.findByNombre("Kilogramo")
                .orElseGet(() -> {
                    UnidadMedida nueva = new UnidadMedida();
                    nueva.setNombre("Kilogramo");
                    nueva.setSimbolo("kg");
                    return unidadMedidaRepository.save(nueva);
                });

        // Categoría de producto
        CategoriaProducto categoria = categoriaProductoRepository.findByNombre("Materia Prima")
                .orElseGet(() -> {
                    CategoriaProducto nueva = new CategoriaProducto();
                    nueva.setNombre("Materia Prima");
                    nueva.setTipo(TipoCategoria.MATERIA_PRIMA);
                    return categoriaProductoRepository.save(nueva);
                });

        // Usuario admin
        Usuario admin = usuarioRepository.findByNombreUsuario("admin")
                .orElseGet(() -> {
                    if (usuarioRepository.existsByCorreo("admin@erp.com")) {
                        throw new IllegalStateException("Ya existe un usuario con el correo 'admin@erp.com'");
                    }
                    Usuario nuevo = new Usuario();
                    nuevo.setNombreUsuario("admin");
                    nuevo.setClave("admin123");
                    nuevo.setNombreCompleto("Administrador");
                    nuevo.setCorreo("admin@erp.com");
                    nuevo.setRol(RolUsuario.ROL_ALMACENISTA);
                    nuevo.setActivo(true);
                    nuevo.setBloqueado(false);
                    return usuarioRepository.save(nuevo);
                });

        // Usuario de prueba
        usuarioRepository.findByNombreUsuario("usuario.test")
                .or(() -> usuarioRepository.findByCorreo("usuario@test.com"))
                .orElseGet(() -> {
                    Usuario usuario = new Usuario();
                    usuario.setNombreUsuario("usuario.test");
                    usuario.setClave("clave123");
                    usuario.setNombreCompleto("Usuario de Prueba");
                    usuario.setCorreo("usuario@test.com");
                    usuario.setRol(RolUsuario.ROL_JEFE_PRODUCCION);
                    usuario.setActivo(true);
                    usuario.setBloqueado(false);
                    return usuarioRepository.save(usuario);
                });

        // Producto
        productoRepository.findByNombre("Producto Test").orElseGet(() -> {
            Producto producto = new Producto();
            producto.setNombre("Producto Test");
            producto.setCodigoSku("SKU-TEST-001");
            producto.setStockActual(new BigDecimal("500"));
            producto.setActivo(true);
            producto.setUnidadMedida(unidadMedidaRepository.findByNombre("Kilogramo").get());
            producto.setCategoriaProducto(categoriaProductoRepository.findByNombre("Materia Prima").get());
            producto.setCreadoPor(usuarioRepository.findByNombreUsuario("admin").get());
            return productoRepository.save(producto);
        });


        // Almacén
        if (!almacenRepository.existsByNombre("Almacén Principal")) {
            Almacen almacen = new Almacen();
            almacen.setNombre("Almacén Principal");
            almacen.setUbicacion("Zona A");
            almacen.setCategoria(TipoCategoria.MATERIA_PRIMA);
            almacen.setTipo(TipoAlmacen.PRINCIPAL);
            almacenRepository.save(almacen);
        }
    }*/

    @Test
    void crearOrdenProduccion_conLoteAsignado_debeRegistrarCorrectamente() throws Exception {
        Producto producto = productoRepository.findByCodigoSku("SKU-BEBIDA-001")
                .orElseThrow(() -> new IllegalStateException("Producto SKU-BEBIDA-001 no encontrado"));

        Usuario usuario = usuarioRepository.findByNombreUsuario("testuser")
                .orElseThrow(() -> new IllegalStateException("Usuario testuser no encontrado"));

        Almacen almacen = almacenRepository.findByNombre("Almacen Central")
                .orElseThrow(() -> new IllegalStateException("Almacén no encontrado"));

        if (!loteProductoRepository.existsByCodigoLote("LOTE-TEST-001")) {
            LoteProducto lote = new LoteProducto();
            lote.setCodigoLote("LOTE-TEST-001");
            lote.setFechaFabricacion(LocalDate.now().minusDays(2));
            lote.setFechaVencimiento(LocalDate.now().plusMonths(3));
            lote.setEstado(EstadoLote.DISPONIBLE);
            lote.setStockLote(new BigDecimal("100.00"));
            lote.setProducto(producto);
            lote.setAlmacen(almacen);
            loteProductoRepository.save(lote);
        }

        OrdenProduccionRequestDTO dto = OrdenProduccionRequestDTO.builder()
                .loteProduccion("LOTE-TEST-001")
                .fechaInicio(LocalDateTime.now())
                .fechaFin(null)
                .cantidadProgramada(100)
                .cantidadProducida(0)
                .estado("CREADA")
                .productoId(producto.getId())
                .responsableId(usuario.getId())
                .build();

        try {
            mockMvc.perform(post("/api/produccion/ordenes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.loteProduccion").value("LOTE-TEST-001"));
        } catch (Exception ex) {
            ex.printStackTrace(); // ✅ Esto imprimirá la causa exacta del error 500
            throw ex;
        }
    }

    @Test
    void asignarLoteAOrdenProduccion_debeGuardarRelacionCorrectamente() throws Exception {
        productoRepository.findAll().forEach(p ->
                System.out.println("📦 Producto: nombre=" + p.getNombre() + " | SKU=" + p.getCodigoSku())
        );

        Producto producto = productoRepository.findByCodigoSku("SKU-BEBIDA-001")
                .orElseThrow(() -> new IllegalStateException("Producto SKU-BEBIDA-001 no encontrado"));

        Usuario usuario = usuarioRepository.findByNombreUsuario("testuser")
                .orElseThrow(() -> new IllegalStateException("Usuario testuser no encontrado"));


        Almacen almacen = almacenRepository.findByNombre("Almacén Principal")
                .orElseGet(() -> {
                    Almacen nuevo = new Almacen();
                    nuevo.setNombre("Almacén Principal");
                    nuevo.setUbicacion("Zona A");
                    nuevo.setCategoria(TipoCategoria.MATERIA_PRIMA);
                    nuevo.setTipo(TipoAlmacen.PRINCIPAL);
                    return almacenRepository.save(nuevo);
                });

        LoteProducto lote = new LoteProducto();
        lote.setCodigoLote("LOTE-TEST-001");
        lote.setFechaFabricacion(LocalDate.now().minusDays(3));
        lote.setFechaVencimiento(LocalDate.now().plusMonths(6));
        lote.setEstado(EstadoLote.DISPONIBLE);
        lote.setStockLote(new BigDecimal("100.00"));
        lote.setProducto(producto);
        lote.setAlmacen(almacen);
        lote = loteProductoRepository.save(lote);

        OrdenProduccionRequestDTO request = OrdenProduccionRequestDTO.builder()
                .productoId(producto.getId())
                .fechaInicio(LocalDateTime.now())
                .loteProduccion(lote.getCodigoLote())
                .responsableId(usuario.getId())
                .estado("CREADA")
                .cantidadProgramada(100)
                .cantidadProducida(0)
                .build();

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loteProduccion").value("LOTE-TEST-001"));

        OrdenProduccion ordenGuardada = ordenProduccionRepository.findByLoteProduccion("LOTE-TEST-001")
                .orElseThrow(() -> new AssertionError("Orden no registrada"));

        assertEquals(lote.getProducto().getId(), ordenGuardada.getProducto().getId());
    }
}
