package no.idporten.eudiw.demo.verifier.crypto;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;

import java.security.PublicKey;
import java.security.spec.ECParameterSpec;

public class ECUtils {

    private ECUtils() {
    }

    public static Curve curveFromKey(PublicKey publicKey) {
        return Curve.forECParameterSpec((ECParameterSpec) publicKey.getParams());
    }

    public static JWSAlgorithm jwsAlgorithmFromKey(PublicKey publicKey) {
        Curve curve = curveFromKey(publicKey);
        if (Curve.P_256.equals(curve)) {
            return JWSAlgorithm.ES256;
        } else if (Curve.SECP256K1.equals(curve)) {
            return JWSAlgorithm.ES256K;
        } else if (Curve.P_384.equals(curve)) {
            return JWSAlgorithm.ES384;
        } else if (Curve.P_521.equals(curve)) {
            return JWSAlgorithm.ES512;
        } else {
            return JWSAlgorithm.EdDSA;
        }
    }

}
