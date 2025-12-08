package no.idporten.eudiw.demo.verifier.openid4vp;

import no.idporten.eudiw.demo.verifier.config.CredentialConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When handling OpenID4VP authorization requests")
@ActiveProfiles("junit")
@SpringBootTest
public class OpenID4VPRequestServiceTest {

    @Autowired
    private OpenID4VPRequestService openID4VPRequestService;

    @DisplayName("then authorization request is build from verification parameters and application configuration")
    @Test
    void testAuthorizationRequestBuiltFromInputAndConfigurationProperties() {
        String verifierTransactionId = "vid";
        CredentialConfig credentialConfig = new CredentialConfig();
        URI authorizationRequest = openID4VPRequestService.createAuthorizationRequest(credentialConfig, verifierTransactionId, "same-device");
        UriComponents uriComponents = UriComponentsBuilder.fromUri(authorizationRequest).build();
        assertAll(
                () -> assertEquals("haip-vp", uriComponents.getScheme()),
                () -> assertEquals("junit.idporten.dev", uriComponents.getHost()),
                () -> assertTrue(uriComponents.getQueryParams().get("client_id").getFirst().startsWith("x509_san_dns:")),
                () -> assertTrue(uriComponents.getQueryParams().containsKey("request_uri")),
                () -> assertTrue(uriComponents.getQueryParams().get("request_uri").getFirst().contains("same-device"))
        );
    }

    @DisplayName("then authorization request is build from verification parameters and application configuration and overrides from credential configuration")
    @Test
    void testAuthorizationRequestBuiltFromInputAndCredentialConfigurationProperties() {
        String verifierTransactionId = "vid";
        CredentialConfig credentialConfig = new CredentialConfig();
        credentialConfig.setAuthorizationRequestUrlScheme("custom-junit-scheme");
        URI authorizationRequest = openID4VPRequestService.createAuthorizationRequest(credentialConfig, verifierTransactionId, "same-device");
        UriComponents uriComponents = UriComponentsBuilder.fromUri(authorizationRequest).build();
        assertAll(
                () -> assertEquals("custom-junit-scheme", uriComponents.getScheme()),
                () -> assertEquals("junit.idporten.dev", uriComponents.getHost()),
                () -> assertTrue(uriComponents.getQueryParams().get("client_id").getFirst().startsWith("x509_san_dns:")),
                () -> assertTrue(uriComponents.getQueryParams().containsKey("request_uri")),
                () -> assertTrue(uriComponents.getQueryParams().get("request_uri").getFirst().contains("same-device"))
        );
    }

}
