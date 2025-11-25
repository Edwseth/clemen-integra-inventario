package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.TipoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.service.MrpReporteService;
import com.willyes.clemenintegra.planeacion.service.MrpService;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MrpController.class)
@AutoConfigureMockMvc(addFilters = false)
class MrpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MrpService mrpService;

    @MockBean
    private PlanProduccionService planProduccionService;

    @MockBean
    private MrpReporteService mrpReporteService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private AuthenticationManager authenticationManager;

    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @Test
    void obtenerIncluyeDatosDeInsumo() throws Exception {
        CategoriaProducto categoria = CategoriaProducto.builder()
                .id(5L)
                .nombre("Envases")
                .build();
        Producto producto = Producto.builder()
                .id(2)
                .codigoSku("SKU-01")
                .nombre("Botella 500ml")
                .categoriaProducto(categoria)
                .build();

        DetalleCorridaMrp detalle = DetalleCorridaMrp.builder()
                .id(10L)
                .producto(producto)
                .requerimientoBruto(BigDecimal.TEN)
                .inventarioDisponible(BigDecimal.ONE)
                .requerimientoNeto(BigDecimal.valueOf(9))
                .build();

        SugerenciaAbastecimiento sugerencia = SugerenciaAbastecimiento.builder()
                .id(11L)
                .detalleCorrida(detalle)
                .tipo(TipoSugerenciaAbastecimiento.COMPRA)
                .cantidadSugerida(BigDecimal.valueOf(9))
                .build();
        detalle.setSugerencia(sugerencia);

        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(3L)
                .semanaInicio(LocalDate.now())
                .semanaFin(LocalDate.now().plusDays(7))
                .build();

        CorridaMrp corrida = CorridaMrp.builder()
                .id(1L)
                .planProduccionSemanal(plan)
                .detalles(List.of(detalle))
                .build();

        when(mrpService.obtenerCorrida(anyLong())).thenReturn(corrida);

        mockMvc.perform(get("/api/mrp/corridas/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].codigoInsumo").value("SKU-01"))
                .andExpect(jsonPath("$.detalles[0].nombreInsumo").value("Botella 500ml"))
                .andExpect(jsonPath("$.detalles[0].categoriaInsumo").value("Envases"))
                .andExpect(jsonPath("$.detalles[0].tipoSugerencia").value("COMPRA"));
    }
}
