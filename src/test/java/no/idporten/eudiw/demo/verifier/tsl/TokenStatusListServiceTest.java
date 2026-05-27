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
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
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

    private static final Instant NOW = Instant.parse("2026-05-27T10:00:00Z");
    private TokenStatuslistService service;
    private MockRestServiceServer mockServer;
    private static final String STATUSLIST = "https://status.eidas2sandkasse.dev/lists/1";
    private static PrivateKey rsaPrivateKey;
    private static X509Certificate rsaCertificate;

    @BeforeAll
    static void initKeyMaterial() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        rsaPrivateKey = keyPair.getPrivate();
        rsaCertificate = selfSignedCertificate(keyPair);
    }

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        TokenStatuslistConfig config = new TokenStatuslistConfig(Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(10_000));
        service = new TokenStatuslistService(restClient, config);
    }

    @Test
    @DisplayName("calls status list endpoint when requesting status list JWT")
    void testRequestStatusListCallsStatusListEndpoint() {
        final String vpToken = "eyJhbGciOiJFUzI1NiIsImtpZCI6IjEyIiwidHlwIjoic3RhdHVzbGlzdCtqd3QifQ.eyJleHAiOjIyOTE3MjAxNzAsImlhdCI6MTY4NjkyMDE3MCwiaXNzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbSIsInN0YXR1c19saXN0Ijp7ImJpdHMiOjEsImxzdCI6ImVOcmJ1UmdBQWhjQlhRIn0sInN1YiI6Imh0dHBzOi8vZXhhbXBsZS5jb20vc3RhdHVzbGlzdHMvMSIsInR0bCI6NDMyMDB9.2lKUUNG503R9htu4aHAYi7vjmr3sgApbfoDvPrl65N3URUO1EYqqQl45Jfzd-Av4QzlKa3oVALpLwOEUOq-U_g";
        mockServer.expect(requestTo(STATUSLIST))
                .andRespond(withSuccess(vpToken, MediaType.parseMediaType("application/statuslist+jwt")));

        service.requestStatusList(URI.create(STATUSLIST));

        mockServer.verify();
    }

    @Test
    @DisplayName("returns VALID when status list JWT is RS256-signed and includes x5c")
    void testCheckStatusReturnsValidForRs256JwtWithX5c() throws Exception {
        Instant now = NOW;
        String statusListJwt = signStatusListJwtWithX5c(STATUSLIST, now.minusSeconds(5), now.plusSeconds(300), 1, compressAndEncode(new byte[]{0}));

        VerificationStatus status = service.checkStatus(URI.create(STATUSLIST), 0, statusListJwt, now);

        assertEquals(VerificationStatus.VALID, status);
    }

    @Test
    @DisplayName("throws VerificationException when status list JWT is missing x5c")
    void testCheckStatusThrowsVerificationExceptionWhenX5cMissing() throws Exception {
        Instant now = NOW;
        String statusListJwt = signStatusListJwtWithoutX5c(STATUSLIST, now.minusSeconds(5), now.plusSeconds(300), 1, compressAndEncode(new byte[]{0}));

        VerificationException e = assertThrows(VerificationException.class,
                () -> service.checkStatus(URI.create(STATUSLIST), 0, statusListJwt, now));

        assertTrue(e.getMessage().contains("x5c"));
    }

    private String signStatusListJwtWithX5c(String subject, Instant iat, Instant exp, int bits, String lst) throws Exception {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(new JOSEObjectType("statuslist+jwt"))
                .x509CertChain(List.of(Base64.encode(rsaCertificate.getEncoded())))
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
        jwt.sign(new RSASSASigner(rsaPrivateKey));
        return jwt.serialize();
    }

    private static X509Certificate selfSignedCertificate(KeyPair keyPair) throws Exception {
        Instant now = Instant.now();
        X500Name dn = new X500Name("CN=statuslist-test");
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dn,
                BigInteger.valueOf(new SecureRandom().nextLong() & Long.MAX_VALUE),
                Date.from(now.minusSeconds(60)),
                Date.from(now.plusSeconds(3600)),
                dn,
                keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certHolder);
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