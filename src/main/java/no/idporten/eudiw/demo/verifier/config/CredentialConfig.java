package no.idporten.eudiw.demo.verifier.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialConfig {

    @NotNull
    private String id;
    @NotNull
    private String docType;
    @NotEmpty
    private List<String> fields;

}
