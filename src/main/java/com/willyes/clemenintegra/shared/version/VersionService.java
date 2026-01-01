package com.willyes.clemenintegra.shared.version;

import com.willyes.clemenintegra.shared.version.dto.VersionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VersionService {

    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final ObjectProvider<GitProperties> gitPropertiesProvider;

    public VersionResponse getVersionInfo() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        GitProperties gitProperties = gitPropertiesProvider.getIfAvailable();

        String gitTag = Optional.ofNullable(gitProperties)
                .map(properties -> properties.get("tags"))
                .filter(tag -> !tag.isBlank())
                .orElse(null);

        String gitCommitId = Optional.ofNullable(gitProperties)
                .map(GitProperties::getShortCommitId)
                .orElse(null);

        if ((gitTag == null || gitTag.isBlank()) && gitCommitId != null) {
            gitTag = gitCommitId;
        }

        Instant buildInstant = buildProperties != null ? buildProperties.getTime() : null;

        return VersionResponse.builder()
                .appName(buildProperties != null ? buildProperties.getName() : "unknown")
                .version(buildProperties != null ? buildProperties.getVersion() : "unknown")
                .buildTime(buildInstant != null ? buildInstant.toString() : null)
                .gitTag(gitTag)
                .gitCommitId(gitCommitId)
                .build();
    }
}
