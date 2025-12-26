package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.service.MrpReporteService;
import com.willyes.clemenintegra.planeacion.service.MrpService;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        MrpController.class,
        MrpControllerSecurityTest.MethodSecurityConfig.class
})
class MrpControllerSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MrpController mrpController;

    @MockBean
    private MrpService mrpService;
    @MockBean
    private PlanProduccionService planProduccionService;
    @MockBean
    private MrpReporteService mrpReporteService;
    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void postCorridaPermiteSuperAdmin() throws Exception {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder().id(1L).build();
        when(planProduccionService.buscarPorId(1L)).thenReturn(Optional.of(plan));
        when(mrpService.ejecutarCorridaSemana(plan)).thenReturn(CorridaMrp.builder().id(5L).build());

        MrpController.CorridaMrpRequest request = new MrpController.CorridaMrpRequest();
        java.lang.reflect.Field planIdField = MrpController.CorridaMrpRequest.class.getDeclaredField("planSemanalId");
        planIdField.setAccessible(true);
        planIdField.set(request, 1L);

        var response = mrpController.ejecutar(request);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void getCorridaPermiteSuperAdmin() throws Exception {
        when(mrpService.obtenerCorrida(9L)).thenReturn(CorridaMrp.builder().id(9L).build());

        var response = mrpController.obtener(9L);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
