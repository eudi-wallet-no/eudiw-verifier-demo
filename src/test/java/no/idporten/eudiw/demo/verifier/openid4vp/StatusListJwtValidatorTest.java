package no.idporten.eudiw.demo.verifier.openid4vp;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.demo.verifier.VerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusListJwtValidatorTest {

    private static final byte[] SIGNING_KEY = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
    private static final Instant NOW = Instant.parse("2026-05-18T10:00:00Z");
    private static final URI STATUS_LIST_URI = URI.create("https://status-provider.example/statuslists/1");

    private StatusListJwtValidator validator;
    private MACVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        validator = new StatusListJwtValidator(Set.of(JWSAlgorithm.HS256), Duration.ofSeconds(30));
        verifier = new MACVerifier(SIGNING_KEY);
    }

    @Test
    @DisplayName("validates known bits=1 vector and resolves expected status values")
    void testKnownVectorBits1() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 1, "eNrbuRgAAhcBXQ");
        int statusAt0 = validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier);
        int statusAt1 = validator.validateAndResolveStatus(STATUS_LIST_URI, 1, jwt, NOW, verifier);
        int statusAt15 = validator.validateAndResolveStatus(STATUS_LIST_URI, 15, jwt, NOW, verifier);

        assertEquals(1, statusAt0);
        assertEquals(0, statusAt1);
        assertEquals(1, statusAt15);
    }

    @Test
    @DisplayName("validates bits=2 extraction from known vector")
    void testBits2Extraction() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 2, "eNo76fITAAPfAgc");
        int statusAt8 = validator.validateAndResolveStatus(STATUS_LIST_URI, 8, jwt, NOW, verifier);
        int statusAt9 = validator.validateAndResolveStatus(STATUS_LIST_URI, 9, jwt, NOW, verifier);
        int statusAt10 = validator.validateAndResolveStatus(STATUS_LIST_URI, 10, jwt, NOW, verifier);

        assertEquals(1, statusAt8);
        assertEquals(2, statusAt9);
        assertEquals(3, statusAt10);
    }

    @Test
    @DisplayName("validates bits=4 extraction from known vector")
    void testBits4Extraction() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 4, "eNrbuRgAAhcBXQ");
        int statusAt0 = validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier);
        int statusAt1 = validator.validateAndResolveStatus(STATUS_LIST_URI, 1, jwt, NOW, verifier);
        int statusAt2 = validator.validateAndResolveStatus(STATUS_LIST_URI, 2, jwt, NOW, verifier);
        int statusAt3 = validator.validateAndResolveStatus(STATUS_LIST_URI, 3, jwt, NOW, verifier);

        assertEquals(9, statusAt0);
        assertEquals(11, statusAt1);
        assertEquals(3, statusAt2);
        assertEquals(10, statusAt3);
    }

    @Test
    @DisplayName("validates bits=8 extraction from known vector")
    void testBits8Extraction() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 8, "eNrbuRgAAhcBXQ");
        int statusAt0 = validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier);
        int statusAt1 = validator.validateAndResolveStatus(STATUS_LIST_URI, 1, jwt, NOW, verifier);

        assertEquals(185, statusAt0);
        assertEquals(163, statusAt1);
    }

    @Test
    @DisplayName("uses required-bytes formula for all allowed bits")
    void testRequiredBytesFormulaForAllowedBits() {
        String lst = compressAndEncode(new byte[16]);
        int idx = 8;

        assertEquals(2, invokeDecodeAndDecompress(lst, idx, 1).length);
        assertEquals(3, invokeDecodeAndDecompress(lst, idx, 2).length);
        assertEquals(5, invokeDecodeAndDecompress(lst, idx, 4).length);
        assertEquals(9, invokeDecodeAndDecompress(lst, idx, 8).length);
    }

    @Test
    @DisplayName("allows decode at max supported required bytes")
    void testRequiredBytesAtMaxAllowed() {
        String lst = compressAndEncode(new byte[1_000_000]);
        assertEquals(1_000_000, invokeDecodeAndDecompress(lst, 999_999, 8).length);
    }

    @Test
    @DisplayName("blocks decode when required bytes exceed max supported size")
    void testRequiredBytesExceedsMax() {
        String lst = compressAndEncode(new byte[1]);
        VerificationException e = assertThrows(VerificationException.class, () ->
                invokeDecodeAndDecompress(lst, 1_000_000, 8));

        assertTrue(e.getMessage().contains("maximum supported range"));
    }

    @Test
    @DisplayName("rejects when signature is invalid")
    void testInvalidSignature() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 1, "eNrbuRgAAhcBXQ");
        MACVerifier wrongVerifier = new MACVerifier("abcdefghijklmnopqrstuvwxyz123456".getBytes(StandardCharsets.UTF_8));

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, wrongVerifier));

        assertTrue(e.getMessage().contains("signature"));
    }

    @Test
    @DisplayName("rejects when typ is not statuslist+jwt")
    void testInvalidTyp() throws Exception {
        String jwt = signJwt("JWT", STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 1, "eNrbuRgAAhcBXQ");

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("typ"));
    }

    @Test
    @DisplayName("rejects when subject does not match referenced uri")
    void testSubMismatch() throws Exception {
        String jwt = signJwt("https://status-provider.example/statuslists/2", NOW.minusSeconds(10), NOW.plusSeconds(300), 1, "eNrbuRgAAhcBXQ");

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("sub"));
    }

    @Test
    @DisplayName("rejects when subject is missing")
    void testMissingSub() throws Exception {
        String jwt = signJwtWithClaims("statuslist+jwt", null, NOW.minusSeconds(10), NOW.plusSeconds(300), Map.of("bits", 1, "lst", "eNrbuRgAAhcBXQ"));

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("sub"));
    }

    @Test
    @DisplayName("rejects when iat is missing")
    void testMissingIat() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), null, NOW.plusSeconds(300), 1, "eNrbuRgAAhcBXQ");

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("iat"));
    }

    @Test
    @DisplayName("rejects when iat is too far in the future")
    void testFutureIatBeyondSkew() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.plusSeconds(120), NOW.plusSeconds(300), 1, "eNrbuRgAAhcBXQ");

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("future"));
    }

    @Test
    @DisplayName("rejects when exp is present and token is expired")
    void testExpiredToken() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(300), NOW.minusSeconds(60), 1, "eNrbuRgAAhcBXQ");

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("accepts token when exp is absent")
    void testWithoutExp() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), null, 1, "eNrbuRgAAhcBXQ");
        int status = validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier);
        assertEquals(1, status);
    }

    @Test
    @DisplayName("rejects when bits is not one of 1,2,4,8")
    void testInvalidBits() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 3, "eNrbuRgAAhcBXQ");

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("bits"));
    }

    @Test
    @DisplayName("rejects bits above 8")
    void testBitsAboveEightRejected() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 16, "eNrbuRgAAhcBXQ");

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("bits"));
    }

    @Test
    @DisplayName("rejects decimal bits even when value is 1.0")
    void testDecimalBitsRejected() throws Exception {
        String jwt = signJwtWithClaims("statuslist+jwt", STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), Map.of("bits", 1.0, "lst", "eNrbuRgAAhcBXQ"));

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("bits"));
    }

    @Test
    @DisplayName("rejects when status_list claim is missing")
    void testMissingStatusList() throws Exception {
        String jwt = signJwtWithClaims("statuslist+jwt", STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), null);

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("status_list"));
    }

    @Test
    @DisplayName("rejects when lst is missing")
    void testMissingLst() throws Exception {
        String jwt = signJwtWithClaims("statuslist+jwt", STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), Map.of("bits", 1));

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("lst"));
    }

    @Test
    @DisplayName("rejects invalid base64url lst")
    void testInvalidBase64Lst() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 1, "not_base64!");

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("base64url"));
    }

    @Test
    @DisplayName("rejects lst that is not a valid zlib stream")
    void testInvalidZlibLst() throws Exception {
        String invalidLst = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[]{0x01, 0x02, 0x03});
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 1, invalidLst);

        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("zlib"));
    }

    @Test
    @DisplayName("rejects idx that is out of bounds")
    void testIdxOutOfBounds() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 1, "eNrbuRgAAhcBXQ");
        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, 16, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("out of bounds"));
    }

    @Test
    @DisplayName("rejects negative idx")
    void testNegativeIdx() throws Exception {
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 1, "eNrbuRgAAhcBXQ");
        VerificationException e = assertThrows(VerificationException.class, () ->
                validator.validateAndResolveStatus(STATUS_LIST_URI, -1, jwt, NOW, verifier));
        assertTrue(e.getMessage().contains("non-negative"));
    }

    @Test
    @DisplayName("rejects jwt signed with disallowed algorithm")
    void testDisallowedAlgorithm() throws Exception {
        StatusListJwtValidator rsOnlyValidator = new StatusListJwtValidator(Set.of(JWSAlgorithm.RS256), Duration.ofSeconds(30));
        String jwt = signJwt(STATUS_LIST_URI.toString(), NOW.minusSeconds(10), NOW.plusSeconds(300), 1, "eNrbuRgAAhcBXQ");

        VerificationException e = assertThrows(VerificationException.class, () ->
                rsOnlyValidator.validateAndResolveStatus(STATUS_LIST_URI, 0, jwt, NOW, verifier));

        assertTrue(e.getMessage().contains("algorithm"));
    }

    private String signJwt(String subject, Instant iat, Instant exp, int bits, String lst) throws Exception {
        return signJwt("statuslist+jwt", subject, iat, exp, bits, lst);
    }

    private String signJwt(String typ, String subject, Instant iat, Instant exp, int bits, String lst) throws Exception {
        return signJwtWithClaims(typ, subject, iat, exp, Map.of("bits", bits, "lst", lst));
    }

    private String signJwtWithClaims(String typ, String subject, Instant iat, Instant exp, Object statusListClaim) throws Exception {
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
        if (subject != null) {
            claimsBuilder.subject(subject);
        }
        if (iat != null) {
            claimsBuilder.issueTime(Date.from(iat));
        }
        if (exp != null) {
            claimsBuilder.expirationTime(Date.from(exp));
        }
        if (statusListClaim != null) {
            claimsBuilder.claim("status_list", statusListClaim);
        }

        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).type(new JOSEObjectType(typ)).build(), claimsBuilder.build());
        jwt.sign(new MACSigner(SIGNING_KEY));
        return jwt.serialize();
    }

    private byte[] invokeDecodeAndDecompress(String lst, int idx, int bits) {
        return (byte[]) ReflectionTestUtils.invokeMethod(validator, "decodeAndDecompress", lst, idx, bits);
    }

    private String compressAndEncode(byte[] input) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             DeflaterOutputStream deflater = new DeflaterOutputStream(output)) {
            deflater.write(input);
            deflater.finish();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
