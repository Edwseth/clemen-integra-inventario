package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.AlmacenRequestDTO;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AlmacenControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AlmacenRepository almacenRepository;

    @BeforeEach
    void setUp() {
        almacenRepository.deleteAll();
    }

    @Test
    void crearAlmacen_PrincipalMateriaPrima_RetornaCreated() throws Exception {
        AlmacenRequestDTO dto = new AlmacenRequestDTO();
        dto.setNombre("Bodega Principal MP");
        dto.setUbicacion("Planta 1");
        dto.setTipo(TipoAlmacen.PRINCIPAL);
        dto.setCategoria(TipoCategoria.MATERIA_PRIMA);

        mockMvc.perform(post("/api/almacenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void crearAlmacen_NombreDuplicado_RetornaConflict() throws Exception {
        AlmacenRequestDTO dto = new AlmacenRequestDTO();
        dto.setNombre("Duplicado");
        dto.setUbicacion("Zona A");
        dto.setTipo(TipoAlmacen.PRINCIPAL);
        dto.setCategoria(TipoCategoria.PRODUCTO_TERMINADO);

        almacenRepository.save(
                com.willyes.clemenintegra.inventario.model.Almacen.builder()
                        .nombre("Duplicado")
                        .ubicacion("Otra")
                        .tipo(TipoAlmacen.SATELITE)
                        .categoria(TipoCategoria.MATERIAL_EMPAQUE)
                        .build()
        );

        mockMvc.perform(post("/api/almacenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void consultarAlmacenes_PorTipoYCategoria_RetornaLista() throws Exception {
        mockMvc.perform(get("/api/almacenes?tipo=PRINCIPAL&categoria=MATERIA_PRIMA"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
