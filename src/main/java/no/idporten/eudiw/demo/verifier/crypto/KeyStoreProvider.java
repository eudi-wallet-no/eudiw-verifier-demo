package no.idporten.eudiw.demo.verifier.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class KeyStoreProvider {

    private static Logger logger = LoggerFactory.getLogger(KeyStoreProvider.class);

    private KeyStore keyStore;

    public KeyStoreProvider(String type, String location, String password, KeystoreResourceLoader resourceLoader) {
        try (InputStream is = resourceLoader.getResource(location).getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(is, password.toCharArray());
            if (logger.isInfoEnabled()) {
                logger.info("Loaded keystore of type {} from {}", type, location.startsWith("base64:") ? String.format("%100.100s...", location) : location);
            }
            this.keyStore = keyStore;
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public KeyStore keyStore() {
        return keyStore;
    }

}
