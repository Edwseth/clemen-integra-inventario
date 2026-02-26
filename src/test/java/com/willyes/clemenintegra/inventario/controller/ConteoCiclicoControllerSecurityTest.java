package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResumenResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import com.willyes.clemenintegra.inventario.service.ConteoCiclicoService;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.support.BaseWebMvcSecurityTest;
import com.willyes.clemenintegra.support.SecurityTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConteoCiclicoController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, ConteoCiclicoControllerSecurityTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class ConteoCiclicoControllerSecurityTest extends BaseWebMvcSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConteoCiclicoService conteoCiclicoService;

    @Test
    void permiteListarConteosConPermisoCanonicoRead() throws Exception {
        ConteoCiclicoResumenResponseDTO response = ConteoCiclicoResumenResponseDTO.builder()
                .id(1L)
                .estado(EstadoConteoCiclico.BORRADOR)
                .build();
        when(conteoCiclicoService.listar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/inventario/conteos")
                        .param("page", "0")
                        .param("size", "10")
                        .with(SecurityTestUtils.userWithAuthorities("contador-permiso", "INV_CONTEOS_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void permiteCrearConteoConPermisoEscritura() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(10L)
                .almacenId(1)
                .estado(EstadoConteoCiclico.BORRADOR)
                .build();
        when(conteoCiclicoService.crearConteo(any(ConteoCiclicoRequestDTO.class))).thenReturn(response);

        ConteoCiclicoRequestDTO request = new ConteoCiclicoRequestDTO();
        request.setAlmacenId(1);

        mockMvc.perform(post("/api/inventario/conteos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityTestUtils.userWithAuthorities("contador-write", "INV_CONTEOS_WRITE")))
                .andExpect(status().isCreated());
    }

    @Test
    void rechazaCrearConteoSoloConPermisoRead() throws Exception {
        ConteoCiclicoRequestDTO request = new ConteoCiclicoRequestDTO();
        request.setAlmacenId(1);

        mockMvc.perform(post("/api/inventario/conteos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityTestUtils.userWithAuthorities("contador-read", "INV_CONTEOS_READ")))
                .andExpect(status().isForbidden());
    }


    @Test
    void rechazaCerrarConteoSinPermisoCloseAunqueTengaWriteYStart() throws Exception {
        mockMvc.perform(post("/api/inventario/conteos/77/cerrar")
                        .with(SecurityTestUtils.userWithAuthorities("jefe-sin-close", "INV_CONTEOS_START")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteCerrarConteoConPermisoClose() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(77L)
                .estado(EstadoConteoCiclico.CERRADO)
                .build();
        when(conteoCiclicoService.cerrar(77L)).thenReturn(response);

        mockMvc.perform(post("/api/inventario/conteos/77/cerrar")
                        .with(SecurityTestUtils.userWithAuthorities("contador-close", "INV_CONTEOS_CLOSE")))
                .andExpect(status().isOk());
    }

    @Test
    void permiteAplicarConteoConPermisoApply() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(77L)
                .estado(EstadoConteoCiclico.APLICADO)
                .build();
        when(conteoCiclicoService.aplicar(anyLong(), eq("k1"))).thenReturn(response);

        mockMvc.perform(post("/api/inventario/conteos/77/aplicar")
                        .header("Idempotency-Key", "k1")
                        .with(SecurityTestUtils.userWithAuthorities("contador", "INV_CONTEOS_APPLY")))
                .andExpect(status().isOk());
    }

    @Test
    void permiteIniciarConteoConPermisoStart() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(55L)
                .estado(EstadoConteoCiclico.EN_CONTEO)
                .build();
        when(conteoCiclicoService.marcarEnConteo(55L)).thenReturn(response);

        mockMvc.perform(post("/api/inventario/conteos/55/iniciar")
                        .with(SecurityTestUtils.userWithAuthorities("jefe-almacen", "INV_CONTEOS_START")))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaAplicarConteoConRolJefeAlmacenes() throws Exception {
        mockMvc.perform(post("/api/inventario/conteos/77/aplicar")
                        .with(SecurityTestUtils.userWithAuthorities("jefe", "ROL_JEFE_ALMACENES")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaAplicarConteoConPermisoWriteSinPermisoApply() throws Exception {
        mockMvc.perform(post("/api/inventario/conteos/77/aplicar")
                        .with(SecurityTestUtils.userWithAuthorities("perm-write", "INV_CONTEOS_WRITE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolJefeAlmacenesPuedeCrearConteoPeroNoAplicarNiCerrar() throws Exception {
        ConteoCiclicoResponseDTO creado = ConteoCiclicoResponseDTO.builder()
                .id(10L)
                .almacenId(1)
                .estado(EstadoConteoCiclico.BORRADOR)
                .build();
        when(conteoCiclicoService.crearConteo(any(ConteoCiclicoRequestDTO.class))).thenReturn(creado);

        ConteoCiclicoRequestDTO request = new ConteoCiclicoRequestDTO();
        request.setAlmacenId(1);

        mockMvc.perform(post("/api/inventario/conteos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityTestUtils.userWithAuthorities("jefe-almacen", "ROL_JEFE_ALMACENES", "INV_CONTEOS_WRITE", "INV_CONTEOS_START")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventario/conteos/10/aplicar")
                        .with(SecurityTestUtils.userWithAuthorities("jefe-almacen", "ROL_JEFE_ALMACENES", "INV_CONTEOS_WRITE", "INV_CONTEOS_START")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/inventario/conteos/10/cerrar")
                        .with(SecurityTestUtils.userWithAuthorities("jefe-almacen", "ROL_JEFE_ALMACENES", "INV_CONTEOS_WRITE", "INV_CONTEOS_START")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolContadorPuedeAplicarYCerrarConteo() throws Exception {
        ConteoCiclicoResponseDTO cerrado = ConteoCiclicoResponseDTO.builder()
                .id(88L)
                .estado(EstadoConteoCiclico.CERRADO)
                .build();
        ConteoCiclicoResponseDTO aplicado = ConteoCiclicoResponseDTO.builder()
                .id(88L)
                .estado(EstadoConteoCiclico.APLICADO)
                .build();
        when(conteoCiclicoService.cerrar(88L)).thenReturn(cerrado);
        when(conteoCiclicoService.aplicar(88L, "k-contador")).thenReturn(aplicado);

        mockMvc.perform(post("/api/inventario/conteos/88/cerrar")
                        .with(SecurityTestUtils.userWithAuthorities("contador", "ROL_CONTADOR", "INV_CONTEOS_CLOSE", "INV_CONTEOS_APPLY")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/inventario/conteos/88/aplicar")
                        .header("Idempotency-Key", "k-contador")
                        .with(SecurityTestUtils.userWithAuthorities("contador", "ROL_CONTADOR", "INV_CONTEOS_CLOSE", "INV_CONTEOS_APPLY")))
                .andExpect(status().isOk());
    }

}
