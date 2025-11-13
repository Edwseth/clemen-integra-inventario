package com.willyes.clemenintegra.inventario.proveedor;

import com.willyes.clemenintegra.inventario.controller.ProveedorController;
import com.willyes.clemenintegra.inventario.mapper.ProveedorMapper;
import com.willyes.clemenintegra.inventario.proveedor.dto.ProveedorAutocompleteDTO;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import com.willyes.clemenintegra.inventario.service.ProveedorService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProveedorController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class ProveedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProveedorService proveedorService;

    @MockBean
    private ProveedorRepository proveedorRepository;

    @MockBean
    private ProveedorMapper proveedorMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    @DisplayName("GET /api/proveedores/autocomplete retorna 200 y contenido paginado cuando term es válido")
    void autocomplete_deberiaRetornarResultados() throws Exception {
        ProveedorAutocompleteDTO dto = ProveedorAutocompleteDTO.builder()
                .id(1L)
                .label("Colombia Insumos")
                .nit("900123456")
                .ciudad("Bogotá")
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<ProveedorAutocompleteDTO> page = new PageImpl<>(List.of(dto), pageable, 1);

        when(proveedorService.buscarAutocomplete(eq("col"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/proveedores/autocomplete")
                        .param("term", "col")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].label").value("Colombia Insumos"))
                .andExpect(jsonPath("$.content[0].nit").value("900123456"))
                .andExpect(jsonPath("$.content[0].ciudad").value("Bogotá"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    @DisplayName("GET /api/proveedores/autocomplete retorna 400 cuando term tiene menos de dos caracteres")
    void autocomplete_deberiaRetornarBadRequestCuandoTermEsCorto() throws Exception {
        mockMvc.perform(get("/api/proveedores/autocomplete")
                        .param("term", "c"))
                .andExpect(status().isBadRequest());
    }
}
