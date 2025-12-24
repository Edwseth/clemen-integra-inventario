package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @SpyBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @SpyBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        unidadMedidaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Un token válido pasa por el filtro y provider permitiendo acceder a endpoints protegidos")
    void tokenValidoDebePermitirAccesoAEndpointProtegido() throws Exception {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombreUsuario("carlos_test")
                .clave("dummy")
                .nombreCompleto("Carlos Test")
                .correo("carlos@test.com")
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .activo(true)
                .bloqueado(false)
                .sessionVersion(1L)
                .ultimaActividad(LocalDateTime.now())
                .build());

        unidadMedidaRepository.save(UnidadMedida.builder()
                .nombre("Kilogramo")
                .simbolo("kg")
                .build());

        String token = jwtTokenService.generarToken(usuario);

        mockMvc.perform(get("/api/unidades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value(equalToIgnoringCase("Kilogramo")));

        verify(jwtAuthenticationFilter, atLeastOnce()).doFilter(any(), any(), any());

        ArgumentCaptor<org.springframework.security.core.Authentication> authenticationCaptor =
                ArgumentCaptor.forClass(org.springframework.security.core.Authentication.class);
        verify(jwtAuthenticationProvider, atLeastOnce()).authenticate(authenticationCaptor.capture());

        assertThat(authenticationCaptor.getAllValues().stream()
                .anyMatch(authentication -> authentication instanceof JwtAuthenticationToken)).isTrue();
    }
}
