package br.com.stormaid.dto.response;

import java.time.Instant;

public record AppVersionResponse(
        String version,
        String commitHash,
        String branch,
        Instant publishedAt,
        String commitUrl
) {
}