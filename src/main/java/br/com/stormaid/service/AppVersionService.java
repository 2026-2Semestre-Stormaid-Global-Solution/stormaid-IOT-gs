package br.com.stormaid.service;

import br.com.stormaid.dto.response.AppVersionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@Service
public class AppVersionService {

    private final String version;
    private final String repositoryUrl;
    private final String configuredPublishedAt;
    private final String renderGitCommit;
    private final String renderGitBranch;

    public AppVersionService(
            @Value("${app.info.version:1.0.0}") String version,
            @Value("${app.info.repository-url:https://github.com/2026-2Semestre-Stormaid-Global-Solution/stormaid-java-gs}") String repositoryUrl,
            @Value("${app.info.published-at:}") String configuredPublishedAt,
            @Value("${render.git.commit:}") String renderGitCommit,
            @Value("${render.git.branch:}") String renderGitBranch
    ) {
        this.version = version;
        this.repositoryUrl = repositoryUrl;
        this.configuredPublishedAt = configuredPublishedAt;
        this.renderGitCommit = renderGitCommit;
        this.renderGitBranch = renderGitBranch;
    }

    public AppVersionResponse getAppInfo() {
        String commit = resolveValue(renderGitCommit, "RENDER_GIT_COMMIT", "local-dev");
        String branch = resolveValue(renderGitBranch, "RENDER_GIT_BRANCH", "local");
        Instant publishedAt = resolvePublishedAt(configuredPublishedAt);
        String commitUrl = buildCommitUrl(repositoryUrl, commit);

        return new AppVersionResponse(
                version,
                commit,
                branch,
                publishedAt,
                commitUrl
        );
    }

    private String resolveValue(String propValue, String envName, String fallback) {
        if (propValue != null && !propValue.isBlank()) {
            return propValue.trim();
        }
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        return fallback;
    }

    private Instant resolvePublishedAt(String rawPublishedAt) {
        if (rawPublishedAt != null && !rawPublishedAt.isBlank()) {
            try {
                return Instant.parse(rawPublishedAt.trim());
            } catch (DateTimeParseException ignored) {
                return Instant.EPOCH;
            }
        }
        return Instant.EPOCH;
    }

    private String buildCommitUrl(String repoUrl, String commit) {
        if (repoUrl == null || repoUrl.isBlank()
                || commit == null || commit.isBlank()
                || "local-dev".equalsIgnoreCase(commit)) {
            return null;
        }
        String cleanRepo = repoUrl.trim().replaceAll("/+$", "");
        return cleanRepo + "/commit/" + commit.trim();
    }
}