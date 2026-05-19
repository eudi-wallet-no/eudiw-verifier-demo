package no.idporten.eudiw.demo.verifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "statuslist")
public record TokenStatuslistConfig (
        @DefaultValue("5s") Duration readTimeout,
        @DefaultValue("3s") Duration connectTimeout
){}

