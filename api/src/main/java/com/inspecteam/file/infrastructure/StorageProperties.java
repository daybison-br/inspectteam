package com.inspecteam.file.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inspecteam.storage")
public record StorageProperties(String endpoint, String bucket, String accessKey, String secretKey) {
}
