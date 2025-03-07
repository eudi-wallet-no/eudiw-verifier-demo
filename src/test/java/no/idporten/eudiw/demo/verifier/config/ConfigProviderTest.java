package no.idporten.eudiw.demo.verifier.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("When reading application configuration")
@ActiveProfiles("test")
@SpringBootTest
public class ConfigProviderTest {

    @Autowired
    private ConfigProvider configProvider;

    @DisplayName("then the client identifier is created from scheme and id")
    @Test
    void testClientIdentifier() {
        String[] parsedClientIdentifier = configProvider.getClientIdentifier().split(":");
        assertAll(
                () -> assertEquals(2, parsedClientIdentifier.length),
                () -> assertEquals("x509_san_dns", parsedClientIdentifier[0]),
                () -> assertEquals("junit.idporten.dev", parsedClientIdentifier[1])
        );

    }

}
