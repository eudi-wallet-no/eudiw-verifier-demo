package no.idporten.eudiw.demo.verifier.crypto;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.util.Base64;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

public class JwksProvider {

    private JWK activeJwk;
    private JWKSet jwkSet;

    public JwksProvider(List<KeyProvider> keyProviders, String use) {
        List<JWK> jwkList = new ArrayList<>();
        try {
            for (KeyProvider keyProvider : keyProviders) {
                List<Base64> encodedCertificates = new ArrayList<>();
                for (Certificate c : keyProvider.certificateChain()) {
                    encodedCertificates.add(Base64.encode(c.getEncoded()));
                }
                if (keyProvider.isRsa()) {
                    jwkList.add(new RSAKey.Builder(keyProvider.rsaPublicKey())
                            .keyUse(KeyUse.parse(use))
                            .keyIDFromThumbprint()
                            .x509CertChain(encodedCertificates)
                            .build());
                } else {
                    jwkList.add(new ECKey.Builder(
                            ECUtils.curveFromKey(keyProvider), keyProvider.ecPublicKey())
                            .keyUse(KeyUse.parse(use))
                            .keyIDFromThumbprint()
                            .x509CertChain(encodedCertificates)
                            .build());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.activeJwk = jwkList.get(0); // first keystore is active and used for signing
        this.jwkSet = new JWKSet(jwkList);
    }

    public JWK activeJwk() {
        return activeJwk;
    }

    public JWKSet jwks() {
        return this.jwkSet;
    }

}
