package no.idporten.wallet.verifier_demo.crypto;

import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;

public class KeyProvider {

    private PrivateKey privateKey;
    private java.security.cert.Certificate certificate;
    private List<java.security.cert.Certificate> certificateChain;

    private PublicKey publicKey;

    public KeyProvider(KeyStore keyStore, String alias, String password) {
        try {
            privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            certificate = keyStore.getCertificate(alias);
            publicKey = certificate.getPublicKey();
            certificateChain = Arrays.asList(keyStore.getCertificateChain(alias));
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

    public PublicKey publicKey() {
        return publicKey;
    }

    public RSAPublicKey rsaPublicKey() {
        return (RSAPublicKey) publicKey();
    }

    public ECPublicKey ecPublicKey() {
        return (ECPublicKey) publicKey();
    }

    public boolean isRsa() {
        return publicKey() instanceof RSAPublicKey;
    }

    public java.security.cert.Certificate certificate() {
        return certificate;
    }

    public List<java.security.cert.Certificate> certificateChain() {
        return certificateChain;
    }
}
