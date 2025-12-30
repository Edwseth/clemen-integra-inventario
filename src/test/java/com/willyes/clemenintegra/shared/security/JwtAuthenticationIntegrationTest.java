package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    void requestProtegidaConTokenValidoDevuelveOk() throws Exception {
        Usuario usuario = Usuario.builder()
                .nombreUsuario("jefealmacen")
                .clave("encoded") // no se usa en el flujo JWT
                .nombreCompleto("Jefe Almacen")
                .correo("jefe@example.com")
                .rol(RolUsuario.ROL_JEFE_ALMACENES)
                .activo(true)
                .bloqueado(false)
                .sessionVersion(1L)
                .ultimaActividad(LocalDateTime.now())
                .build();

        usuario = usuarioRepository.saveAndFlush(usuario);
        String token = jwtTokenService.generarToken(usuario);

        mockMvc.perform(get("/api/unidades")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
