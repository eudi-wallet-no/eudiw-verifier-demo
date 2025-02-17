package no.idporten.eudiw.demo.verifier.crypto;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.InputStreamResource;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class KeystoreResourceLoader extends DefaultResourceLoader {


    public KeystoreResourceLoader() {
        super();
        addBase64ResourceLoader();
    }

    public KeystoreResourceLoader(ClassLoader classLoader) {
        super(classLoader);
        addBase64ResourceLoader();
    }

    /**
     * Creates resource from base64-encoded strings prefixed with "base64:".
     */
    private void addBase64ResourceLoader() {
        this.addProtocolResolver((location, resourceLoader) -> {
            if (location.startsWith("base64:")) {
                return new InputStreamResource(new ByteArrayInputStream(Base64.getDecoder().decode(location.substring(location.indexOf(":") + 1).trim().getBytes())));
            } else {
                return null;
            }
        });
    }


}
