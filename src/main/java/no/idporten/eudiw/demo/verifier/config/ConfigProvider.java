package no.idporten.eudiw.demo.verifier.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "vcverifier")
@Validated
public class ConfigProvider {

    @NotNull
    private String siop2ClientId;

    @NotNull
    private String clientIdentifierScheme;

    @NotNull
    private String authorizationRequestUrlScheme;

    @NotNull
    private String externalBaseUrl;
    @NotEmpty
    private List<@Valid CredentialConfig> credentialConfigurations = new ArrayList<>();

    public CredentialConfig getCredentialConfig(String id) {
        return credentialConfigurations.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    public String getClientIdentifier() {
        return String.format("%s:%s", clientIdentifierScheme, siop2ClientId);
    }

}
