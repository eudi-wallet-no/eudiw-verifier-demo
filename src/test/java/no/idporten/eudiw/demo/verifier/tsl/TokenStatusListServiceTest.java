package no.idporten.eudiw.demo.verifier.tsl;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.demo.verifier.VerificationException;
import no.idporten.eudiw.demo.verifier.config.TokenStatuslistConfig;
import no.idporten.eudiw.demo.verifier.web.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TokenStatusListServiceTest {

    private TokenStatuslistService service;
    private MockRestServiceServer mockServer;
    private static final String STATUSLIST = "https://status.eidas2sandkasse.dev/lists/1";
    private static final String RSA_PRIVATE_KEY_RESOURCE = "fixtures/statuslist-test-private-key.pem";
    private static final String RSA_CERT_RESOURCE = "fixtures/statuslist-test-certificate.pem";

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        TokenStatuslistConfig config = new TokenStatuslistConfig(Duration.ofSeconds(3), Duration.ofSeconds(3), List.of("status.eidas2sandkasse.dev"));
        service = new TokenStatuslistService(restClient, config);
    }

    @Test
    void testRestClientCall() {
        final String vpToken = "eyJhbGciOiJFUzI1NiIsImtpZCI6IjEyIiwidHlwIjoic3RhdHVzbGlzdCtqd3QifQ.eyJleHAiOjIyOTE3MjAxNzAsImlhdCI6MTY4NjkyMDE3MCwiaXNzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbSIsInN0YXR1c19saXN0Ijp7ImJpdHMiOjEsImxzdCI6ImVOcmJ1UmdBQWhjQlhRIn0sInN1YiI6Imh0dHBzOi8vZXhhbXBsZS5jb20vc3RhdHVzbGlzdHMvMSIsInR0bCI6NDMyMDB9.2lKUUNG503R9htu4aHAYi7vjmr3sgApbfoDvPrl65N3URUO1EYqqQl45Jfzd-Av4QzlKa3oVALpLwOEUOq-U_g";
        mockServer.expect(requestTo(STATUSLIST))
                .andRespond(withSuccess(vpToken, MediaType.parseMediaType("application/statuslist+jwt")));

        service.requestStatusList(URI.create(STATUSLIST));

        mockServer.verify();
    }

    @Test
    void checkStatus_validRs256JwtWithX5c_returnsValid() throws Exception {
        Instant now = Instant.now();
        String statusListJwt = signStatusListJwtWithX5c(STATUSLIST, now.minusSeconds(5), now.plusSeconds(300), 1, compressAndEncode(new byte[]{0}));

        VerificationStatus status = service.checkStatus(URI.create(STATUSLIST), 0, statusListJwt, now);

        assertEquals(VerificationStatus.VALID, status);
    }

    @Test
    void checkStatus_missingX5c_throwsVerificationException() throws Exception {
        Instant now = Instant.now();
        String statusListJwt = signStatusListJwtWithoutX5c(STATUSLIST, now.minusSeconds(5), now.plusSeconds(300), 1, compressAndEncode(new byte[]{0}));

        VerificationException e = assertThrows(VerificationException.class,
                () -> service.checkStatus(URI.create(STATUSLIST), 0, statusListJwt, now));

        assertTrue(e.getMessage().contains("x5c"));
    }

    @Test
    void checkStatus_disallowedHost_throwsVerificationException() throws Exception {
        Instant now = Instant.now();
        String statusListJwt = signStatusListJwtWithX5c("https://attacker.example/lists/1", now.minusSeconds(5), now.plusSeconds(300), 1, compressAndEncode(new byte[]{0}));

        VerificationException e = assertThrows(VerificationException.class,
                () -> service.checkStatus(URI.create("https://attacker.example/lists/1"), 0, statusListJwt, now));

        assertTrue(e.getMessage().contains("host is not allowed"));
    }

    private String signStatusListJwtWithX5c(String subject, Instant iat, Instant exp, int bits, String lst) throws Exception {
        X509Certificate cert = parseCertificate(loadResourceAsText(RSA_CERT_RESOURCE));
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(new JOSEObjectType("statuslist+jwt"))
                .x509CertChain(List.of(Base64.encode(cert.getEncoded())))
                .build();
        return signStatusListJwt(header, subject, iat, exp, bits, lst);
    }

    private String signStatusListJwtWithoutX5c(String subject, Instant iat, Instant exp, int bits, String lst) throws Exception {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(new JOSEObjectType("statuslist+jwt"))
                .build();
        return signStatusListJwt(header, subject, iat, exp, bits, lst);
    }

    private String signStatusListJwt(JWSHeader header, String subject, Instant iat, Instant exp, int bits, String lst) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(Date.from(iat))
                .expirationTime(Date.from(exp))
                .claim("status_list", Map.of("bits", bits, "lst", lst))
                .build();

        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(new RSASSASigner(parsePrivateKey(loadResourceAsText(RSA_PRIVATE_KEY_RESOURCE))));
        return jwt.serialize();
    }

    private String loadResourceAsText(String path) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test resource: " + path);
            }
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private X509Certificate parseCertificate(String pem) throws Exception {
        String base64 = pem.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
        byte[] certBytes = java.util.Base64.getDecoder().decode(base64);
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(certBytes));
    }

    private PrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = java.util.Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private String compressAndEncode(byte[] input) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             DeflaterOutputStream deflater = new DeflaterOutputStream(output)) {
            deflater.write(input);
            deflater.finish();
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}