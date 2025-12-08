package no.idporten.eudiw.demo.verifier.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.util.List;

@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialConfig implements Serializable {

    @NotNull
    private String id;
    @NotNull
    private String docType;
    @NotNull
    private String format;
    @NotEmpty
    private List<String> claims;

    // Alternativ for conformance - poc
    private String authorizationEndpointUri;
    // Override authorization request uri cheme
    private String authorizationRequestUrlScheme;

}
