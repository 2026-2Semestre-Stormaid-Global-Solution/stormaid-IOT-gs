package br.com.stormaid.service;

import br.com.stormaid.dto.response.AppVersionResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppVersionServiceTest {

    private static final String REPO_URL =
            "https://github.com/2026-2Semestre-Stormaid-Global-Solution/stormaid-java-gs";

    @Test
    void shouldReturnPredictableLocalValuesWhenRenderVarsAreMissing() {
        AppVersionService service = new AppVersionService(
                "1.2.0",
                REPO_URL,
                "",
                "",
                ""
        );

        AppVersionResponse response = service.getAppInfo();

        assertEquals("1.2.0", response.version());
        assertEquals("local-dev", response.commitHash());
        assertEquals("local", response.branch());
        assertEquals(Instant.EPOCH, response.publishedAt());
        assertNull(response.commitUrl());
    }

    @Test
    void shouldReturnFullCommitHashBranchPublishedAtAndCommitUrlForRender() {
        String fullHash = "9f8e7d6c5b4a3210fedcba9876543210abcdef12";
        String published = "2026-11-19T18:30:00Z";

        AppVersionService service = new AppVersionService(
                "1.2.0",
                REPO_URL,
                published,
                fullHash,
                "main"
        );

        AppVersionResponse response = service.getAppInfo();

        assertEquals("1.2.0", response.version());
        assertEquals(fullHash, response.commitHash());
        assertEquals("main", response.branch());
        assertEquals(Instant.parse(published), response.publishedAt());
        assertEquals(REPO_URL + "/commit/" + fullHash, response.commitUrl());
    }
}