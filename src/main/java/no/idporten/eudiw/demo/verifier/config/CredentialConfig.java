package no.idporten.eudiw.demo.verifier.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialConfig {

    @NotNull
    private String id;
    @NotNull
    private String title;
    @NotNull
    private String description;
    @NotNull
    private String docType;
    @NotEmpty
    private List<String> claims;
    @NotEmpty
    private Map<String, String> claimDescriptions;

}
