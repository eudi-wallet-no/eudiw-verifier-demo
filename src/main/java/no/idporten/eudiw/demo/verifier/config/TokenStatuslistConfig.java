package no.idporten.eudiw.demo.verifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "statuslist")
public record TokenStatuslistConfig (
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue({"status.eidas2sandkasse.dev", "status.test.eidas2sandkasse.net"}) List<String> allowedHosts
){}
