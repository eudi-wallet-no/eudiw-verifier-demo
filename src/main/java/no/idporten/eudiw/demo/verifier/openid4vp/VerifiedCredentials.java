package no.idporten.eudiw.demo.verifier.openid4vp;


import java.io.Serializable;
import java.util.Map;

/**
 * Parsed verified credentials.
 */
public record VerifiedCredentials (String vpToken, Map<String, Object> credentials) implements Serializable {
}
