package no.idporten.eudiw.demo.verifier.openid4vp;

import com.nimbusds.jose.jwk.JWK;
import lombok.Builder;
import lombok.Data;
import no.idporten.eudiw.demo.verifier.config.CredentialConfig;
import no.idporten.eudiw.demo.verifier.trace.ProtocolTrace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class VerificationTransaction implements Serializable {

    private String status;
    private CredentialConfig credentialConfiguration;
    private String flow;
    private String state;
    private String nonce;
    private JWK encryptionKey;
    private VerifiedCredentials verifiedCredentials;
    private List<ProtocolTrace>  protocolTraces = new ArrayList<>();

    public void addProtocolTrace(ProtocolTrace protocolTrace) {
        protocolTraces.add(protocolTrace);
    }

}
