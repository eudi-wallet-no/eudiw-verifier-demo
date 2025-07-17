package no.idporten.eudiw.demo.verifier.service.no.idporten.eudiw.demo.verifier.service;

import no.idporten.eudiw.demo.verifier.service.OID4VPRequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When generating wallet requests")
@ActiveProfiles("junit")
@SpringBootTest
public class OID4VPRequestServiceTest {

    @Autowired
    private OID4VPRequestService oid4VPRequestService;

    @DisplayName("then the client identifier scheme is appended to the client id")
    @Test
    void testGenerateAuthorizationRequest() {
        String autzRequest = oid4VPRequestService.getAuthorizationRequest("pid", "california");
        UriComponents authzRequestComponents = UriComponentsBuilder.fromUriString(autzRequest).build();
        assertAll(
                () -> assertEquals("eudi-openid4vp", authzRequestComponents.getScheme()),
                () -> assertEquals("junit.idporten.dev", authzRequestComponents.getHost()),
                () -> assertNull(authzRequestComponents.getQueryParams().get("client_id_scheme"))
        );
        UriComponents requestUriComponents = UriComponentsBuilder.fromUriString(authzRequestComponents.getQueryParams().get("request_uri").getFirst()).build();
        assertAll(
                () -> assertEquals("junit.idporten.dev", requestUriComponents.getHost()),
                () -> assertEquals("req", requestUriComponents.getPathSegments().get(0)),
                () -> assertEquals("pid", requestUriComponents.getPathSegments().get(1)),
                () -> assertEquals("california", requestUriComponents.getPathSegments().get(2))
        );
    }

}
