package no.idporten.eudiw.demo.verifier.config;

import no.idporten.eudiw.demo.verifier.crypto.JwksProvider;
import no.idporten.eudiw.demo.verifier.crypto.KeyProvider;
import no.idporten.eudiw.demo.verifier.crypto.KeyStoreProvider;
import no.idporten.eudiw.demo.verifier.crypto.KeystoreResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SpringConfig {

    @Bean
    public KeyProvider keyProvider(ConfigProvider configProvider) {
        ConfigProvider.Keystore keystoreConfig = configProvider.activeKeystore();
        KeyStoreProvider keyStoreProvider = new KeyStoreProvider(keystoreConfig.getType(), keystoreConfig.getLocation(), keystoreConfig.getPassword(), new KeystoreResourceLoader());
        return new KeyProvider(keyStoreProvider.keyStore(), keystoreConfig.getKeyAlias(), keystoreConfig.getKeyPassword());
    }

    @Bean
    public JwksProvider jwkProvider(ConfigProvider configProvider) {
        List<KeyProvider> keyProviders = configProvider.getKeystores().stream()
                .map(keystoreConfig -> new KeyProvider(
                        new KeyStoreProvider(
                                keystoreConfig.getType(),
                                keystoreConfig.getLocation(),
                                keystoreConfig.getPassword(),
                                new KeystoreResourceLoader())
                                .keyStore(),
                        keystoreConfig.getKeyAlias(),
                        keystoreConfig.getKeyPassword())).toList();
        return new JwksProvider(keyProviders, "sig");
    }

}
