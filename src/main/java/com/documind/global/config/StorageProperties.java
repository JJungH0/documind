package com.documind.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "documind.storage")
public record StorageProperties (String location){
}
