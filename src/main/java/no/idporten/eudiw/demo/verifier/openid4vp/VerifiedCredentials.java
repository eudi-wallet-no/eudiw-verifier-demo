package no.idporten.eudiw.demo.verifier.openid4vp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
public class VerifiedCredentials implements Serializable {
    private String vpToken;
    private Map<String, Object> credentials;
}
