package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UsuarioRepository;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
@Transactional
class OrdenProduccionServiceTest {

    @Autowired
    private OrdenProduccionService ordenProduccionService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private OrdenProduccionRepository ordenProduccionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void guardarConValidacionStock_debeDescontarStockYGuardarOrden() {
        // 1. Buscar producto final (bebida enriquecida)
        Producto productoFinal = productoRepository.findByCodigoSku("SKU-BEBIDA-001")
                .orElseThrow(() -> new RuntimeException("Producto final no encontrado"));

        Usuario responsable = usuarioRepository.findByNombreUsuario("testuser")
                .orElseThrow(() -> new RuntimeException("Usuario responsable no encontrado"));

        // 2. Crear la orden
        OrdenProduccion orden = OrdenProduccion.builder()
                .producto(productoFinal)
                .responsable(responsable)
                .cantidadProgramada(300)
                .loteProduccion("LOTE-TEST-001")
                .fechaInicio(LocalDateTime.now())
                .build();


        // 3. Ejecutar lógica
        OrdenProduccion resultado = ordenProduccionService.guardarConValidacionStock(orden);

        // 4. Validar que la orden se guardó
        assertThat(resultado.getId()).isNotNull();

        // 5. Validar que el stock de Agua Purificada se descontó correctamente
        Producto agua = productoRepository.findByCodigoSku("PV000")
                .orElseThrow(() -> new RuntimeException("Insumo Agua no encontrado"));

        assertThat(agua.getStockActual()).isEqualByComparingTo(new BigDecimal("43.867")); // 300 - 256.133
    }

    @Test
    void guardarConInsumoInsuficiente_debeFallar() {
        // 1. Configurar insumo con stock muy bajo
        Producto agua = productoRepository.findByCodigoSku("PV000")
                .orElseThrow(() -> new RuntimeException("Insumo Agua no encontrado"));
        agua.setStockActual(new BigDecimal("10")); // Muy por debajo de los 256.133 necesarios
        productoRepository.save(agua);

        // 2. Buscar producto final y usuario
        Producto productoFinal = productoRepository.findByCodigoSku("SKU-BEBIDA-001")
                .orElseThrow(() -> new RuntimeException("Producto final no encontrado"));

        Usuario responsable = usuarioRepository.findByNombreUsuario("testuser")
                .orElseThrow(() -> new RuntimeException("Usuario responsable no encontrado"));

        // 3. Construir orden de producción con fórmula ya aprobada
        OrdenProduccion orden = OrdenProduccion.builder()
                .producto(productoFinal)
                .responsable(responsable)
                .cantidadProgramada(300)
                .fechaInicio(LocalDateTime.now())
                .loteProduccion("LOTE-INSUFICIENTE-001")
                .build();

        // 4. Ejecutar y verificar que falle
        assertThrows(IllegalStateException.class, () -> {
            ordenProduccionService.guardarConValidacionStock(orden);
        });
    }

}

