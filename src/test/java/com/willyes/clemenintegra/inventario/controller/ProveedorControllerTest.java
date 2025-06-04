package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.bom.repository.DetalleFormulaRepository;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.dto.ProveedorRequestDTO;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class ProveedorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired LoteProductoRepository loteProductoRepository;
    @Autowired private OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private OrdenCompraRepository ordenCompraRepository;
    @Autowired private AjusteInventarioRepository ajusteInventarioRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private FormulaProductoRepository formulaProductoRepository;
    @Autowired private DetalleFormulaRepository detalleFormulaRepository;
    @Autowired private OrdenProduccionRepository ordenProduccionRepository;

    @BeforeEach
    void setup() {
        loteProductoRepository.deleteAll();
        ajusteInventarioRepository.deleteAll();
        ordenCompraDetalleRepository.deleteAll();
        ordenCompraRepository.deleteAll();
        formulaProductoRepository.deleteAll();
        detalleFormulaRepository.deleteAll();
        ordenProduccionRepository.deleteAll();
        proveedorRepository.deleteAll();
        productoRepository.deleteAll();
    }

    @Test
    void crearProveedor_valido_debeRetornarCreated() throws Exception {
        ProveedorRequestDTO dto = new ProveedorRequestDTO(
                "Laboratorios Nova",
                "901234567",
                "3100000000",
                "nova@correo.com",
                "Calle 50 #10-15",
                "https://nova.com",
                "Juan Pérez",
                true
        );

        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Laboratorios Nova"));
    }

    @Test
    void crearProveedor_identificacionODuplicada_debeRetornarConflict() throws Exception {
        // Proveedor base
        proveedorRepository.save(Proveedor.builder()
                .nombre("Proveedor Existente")
                .identificacion("123456789")
                .email("existente@correo.com")
                .nombreContacto("Contacto")
                .telefono("3000000000")
                .direccion("Calle falsa")
                .activo(true)
                .build());

        ProveedorRequestDTO duplicado = new ProveedorRequestDTO(
                "Nuevo Proveedor",         // nombre
                "123456789",               // identificacion (duplicado)
                "3101234567",              // telefono (puede ser cualquier valor)
                "existente@correo.com",    // email (duplicado)
                "Carrera 20",              // dirección
                null,                      // página web
                "Nuevo Contacto",          // nombreContacto
                true                       // activo
        );

        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicado)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un proveedor con esa identificación o correo"))
                .andExpect(jsonPath("$.status").value(409));


    }
}

