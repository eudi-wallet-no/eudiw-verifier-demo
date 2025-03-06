package no.idporten.eudiw.demo.verifier.crypto;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;

public class ECUtils {

    private ECUtils() {
    }

    public static Curve curveFromKey(KeyProvider keyProvider) {
        return Curve.forECParameterSpec(keyProvider.ecPublicKey().getParams());
    }

    public static JWSAlgorithm jwsAlgorithmFromKey(KeyProvider keyProvider) {
        Curve curve = curveFromKey(keyProvider);
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
