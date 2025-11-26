package no.idporten.eudiw.demo.verifier.openid4vp;

import com.nimbusds.jose.jwk.JWK;
import lombok.Data;
import no.idporten.eudiw.demo.verifier.config.CredentialConfig;
import no.idporten.eudiw.demo.verifier.trace.ProtocolTrace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data store for verification transaction attributes.
 */
@Data
public class VerificationTransaction implements Serializable {

    // the config base for authz request
    private CredentialConfig credentialConfiguration;

    // transaction status
    private String status = "WAIT";

    // transaction features
    private String flow;

    // transaction protocol verifiers
    private String state;
    private String nonce;
    private JWK encryptionKey;

    // transaction result
    private VerifiedCredentials verifiedCredentials;

    // traced protocol interactions
    private List<ProtocolTrace>  protocolTraces = new ArrayList<>();

    public void setVerifiedCredentials(VerifiedCredentials verifiedCredentials) {
        this.verifiedCredentials = verifiedCredentials;
        if (verifiedCredentials != null) {
            setStatus("AVAILABLE");
        }
    }

    public void addProtocolTrace(ProtocolTrace protocolTrace) {
        protocolTraces.add(protocolTrace);
    }

}
