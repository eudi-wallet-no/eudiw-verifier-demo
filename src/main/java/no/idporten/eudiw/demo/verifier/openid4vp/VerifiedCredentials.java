package no.idporten.eudiw.demo.verifier.openid4vp;


import no.idporten.eudiw.demo.verifier.web.VerificationStatus;

import java.io.Serializable;
import java.util.Map;

/**
 * Parsed verified credentials.
 */
public record VerifiedCredentials (String vpToken, Map<String, Object> credentials, VerificationStatus status) implements Serializable {
}
