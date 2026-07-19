package com.inspecteam.form.domain;

import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record PublishedFormDetails(UUID id, String name, String description,
        UUID versionId, int version, JsonNode definition) {
}
