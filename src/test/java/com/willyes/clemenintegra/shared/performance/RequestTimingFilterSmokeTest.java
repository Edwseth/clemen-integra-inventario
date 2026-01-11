package com.willyes.clemenintegra.shared.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.support.IntegrationTestMySqlContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class RequestTimingFilterSmokeTest extends IntegrationTestMySqlContainer {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void logsRequestTimingForPublicEndpoint(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/public/version"))
                .andExpect(status().isOk());

        assertThat(output.getOut()).contains("RequestTiming method=GET uri=/api/public/version");
    }
}
