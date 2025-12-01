package com.willyes.clemenintegra.shared.version.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponse {
    private String appName;
    private String version;
    private String buildTime;
    private String gitTag;
    private String gitCommitId;
}
