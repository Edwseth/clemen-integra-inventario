package com.willyes.clemenintegra.shared.version;

import com.willyes.clemenintegra.shared.version.dto.VersionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/version")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    @GetMapping
    public VersionResponse getVersion() {
        return versionService.getVersionInfo();
    }
}
