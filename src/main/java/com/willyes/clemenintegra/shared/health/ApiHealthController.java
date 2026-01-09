package com.willyes.clemenintegra.shared.health;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class ApiHealthController {

    @Value("${spring.application.name:clemen-integra-erp}")
    private String applicationName;

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("application", applicationName);
        response.put("timestamp", Instant.now().toString());
        return response;
    }
}
